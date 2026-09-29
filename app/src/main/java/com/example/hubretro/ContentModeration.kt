package com.example.hubretro

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

// ═══════════════════════════════════════════════════════════════════════════════
// CONTENT MODERATION
//
// Policy (applies to chat, posts, comments, articles, checkpoints, stories, usernames):
//   • Curse words   → blurred (▒▒▒▒), message still sent, player told to keep it clean.
//   • Racial slurs  → blurred + FINAL WARNING. Second time → account banned.
//   • Sexual content→ message NOT sent + warning. Ignoring the warning → account banned.
//
// Strikes/ban live on users/{uid}: modSlurStrikes, modSexualStrikes, banned, bannedReason.
// NOTE: this is client-side. For real enforcement also add the Firestore rule in the
// chat reply notes (banned users can't write), and ideally a server-side check.
// ═══════════════════════════════════════════════════════════════════════════════

object Moderation {

    private enum class Mode { EXACT, PREFIX, CONTAINS }
    private data class Term(val word: String, val mode: Mode)

    private fun exact(vararg w: String) = w.map { Term(it, Mode.EXACT) }
    private fun prefix(vararg w: String) = w.map { Term(it, Mode.PREFIX) }
    private fun contains(vararg w: String) = w.map { Term(it, Mode.CONTAINS) }

    // Curse words (light) — blurred
    private val profanity: List<Term> =
        contains("fuck", "shit", "motherf", "bullsh", "asshole", "dipshit", "jackass", "dumbass") +
        prefix("bitch", "bastard", "cunt", "piss", "twat", "wank", "bollock", "douche", "slut", "whore") +
        exact("ass", "asses", "damn", "damnit", "goddamn", "wtf", "stfu", "fck", "fk", "crap", "arse", "dick", "dicks", "dickhead", "prick", "pricks")

    // Sexual content — blocked
    private val sexual: List<Term> =
        contains("porn", "blowjob", "handjob", "onlyfans", "hentai", "dildo", "masturbat", "orgasm", "sexting", "nudes") +
        prefix("sexy", "horny", "titties", "pussy", "penis", "vagina", "erotic", "fetish", "nsfw", "xxx", "cumming") +
        exact("sex", "nude", "naked", "cum", "anal", "milf", "boner", "boob", "boobs", "boobies", "tits")

    // Racial / hate slurs — blurred + final warning, then ban
    private val slurs: List<Term> =
        contains("nigg", "faggot", "sandnig") +
        prefix("negro", "retard", "tranny", "wetback", "raghead", "towelhead", "chink", "kike", "beaner", "spick") +
        exact("fag", "fags", "dyke", "gook", "spic", "coon", "coons", "paki")

    private val leet = mapOf('0' to 'o', '1' to 'i', '!' to 'i', '|' to 'i', '3' to 'e', '4' to 'a', '@' to 'a',
        '5' to 's', '$' to 's', '7' to 't', '8' to 'b', '9' to 'g', '+' to 't')

    /** lowercase, leetspeak → letters, drop symbols; also a version with repeated letters collapsed. */
    private fun normalize(token: String): List<String> {
        val base = token.lowercase().map { leet[it] ?: it }.filter { it.isLetter() }.joinToString("")
        if (base.isEmpty()) return emptyList()
        val collapsed = base.replace(Regex("(.)\\1+"), "$1")          // fuuuuck → fuck
        val twoMax = base.replace(Regex("(.)\\1{2,}"), "$1$1")         // asssss → ass
        return listOf(base, twoMax, collapsed).distinct()
    }

    private fun hits(forms: List<String>, list: List<Term>): Boolean = forms.any { f ->
        list.any { t ->
            when (t.mode) {
                Mode.EXACT -> f == t.word
                Mode.PREFIX -> f.startsWith(t.word)
                Mode.CONTAINS -> f.contains(t.word)
            }
        }
    }

    enum class Kind { PROFANITY, SLUR, SEXUAL }

    data class Scan(val cleaned: String, val profanity: Boolean, val slur: Boolean, val sexual: Boolean) {
        val clean get() = !profanity && !slur && !sexual
    }

    private val tokenRegex = Regex("[\\p{L}\\p{N}@$!|+*]+")

    fun scan(text: String): Scan {
        val chars = text.toCharArray()
        var prof = false; var slur = false; var sex = false
        val tokens = tokenRegex.findAll(text).toList()

        fun maskRange(range: IntRange) { for (i in range) if (!chars[i].isWhitespace()) chars[i] = '▒' }

        tokens.forEach { m ->
            val forms = normalize(m.value)
            when {
                hits(forms, slurs) -> { slur = true; maskRange(m.range) }
                hits(forms, sexual) -> { sex = true; maskRange(m.range) }
                hits(forms, profanity) -> { prof = true; maskRange(m.range) }
            }
        }
        // Spaced-out evasion: "f u c k", "n i g ..."
        var i = 0
        while (i < tokens.size) {
            var j = i
            while (j < tokens.size && tokens[j].value.length == 1) j++
            if (j - i >= 3) {
                val joined = tokens.subList(i, j).joinToString("") { it.value }
                val forms = normalize(joined)
                val range = tokens[i].range.first..tokens[j - 1].range.last
                when {
                    hits(forms, slurs) -> { slur = true; maskRange(range) }
                    hits(forms, sexual) -> { sex = true; maskRange(range) }
                    hits(forms, profanity) -> { prof = true; maskRange(range) }
                }
            }
            i = maxOf(j, i + 1)
        }
        return Scan(String(chars), prof, slur, sex)
    }

    fun isAllowedName(name: String) = scan(name).clean

    // ── Strike state (kept in sync with Firestore by ModerationWatcher) ──────────
    var slurStrikes by mutableIntStateOf(0)
    var sexualStrikes by mutableIntStateOf(0)
    var banned by mutableStateOf(false)
    var bannedReason by mutableStateOf("")

    private fun userRef() = FirebaseAuth.getInstance().currentUser?.uid?.let {
        FirebaseFirestore.getInstance().collection("users").document(it)
    }

    private fun record(fields: Map<String, Any>) {
        userRef()?.set(fields, SetOptions.merge())
    }

    private fun ban(reason: String, where: String) {
        banned = true
        bannedReason = reason
        record(mapOf("banned" to true, "bannedReason" to reason, "bannedAt" to System.currentTimeMillis(), "bannedWhere" to where))
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance().collection("moderation_bans").document(uid)
                .set(mapOf("uid" to uid, "reason" to reason, "where" to where, "timestamp" to System.currentTimeMillis()))
        }
        ModerationBus.show(ModerationNotice.BANNED)
    }

    /**
     * Run EVERY user-written text through this before saving it.
     * Returns the text to save (possibly blurred), or null if it must NOT be sent.
     * Shows the right warning dialog automatically.
     */
    fun gate(text: String, where: String): String? {
        if (banned) { ModerationBus.show(ModerationNotice.BANNED); return null }
        val s = scan(text)
        if (s.clean) return text
        return when {
            s.sexual -> {
                if (sexualStrikes >= 1) { ban("Sexual content after a warning", where); null }
                else {
                    sexualStrikes = 1
                    record(mapOf("modSexualStrikes" to FieldValue.increment(1), "modLastViolation" to System.currentTimeMillis()))
                    ModerationBus.show(ModerationNotice.SEXUAL_BLOCKED)
                    null
                }
            }
            s.slur -> {
                if (slurStrikes >= 1) { ban("Hate speech / racial slurs after a warning", where); null }
                else {
                    slurStrikes = 1
                    record(mapOf("modSlurStrikes" to FieldValue.increment(1), "modLastViolation" to System.currentTimeMillis()))
                    ModerationBus.show(ModerationNotice.SLUR_WARNING)
                    s.cleaned
                }
            }
            else -> {
                ModerationBus.show(ModerationNotice.PROFANITY_BLURRED)
                s.cleaned
            }
        }
    }

    /** Checks several fields at once (e.g. title + body). Returns cleaned fields in the same order, or null if blocked. */
    fun gateAll(vararg texts: String, where: String): List<String>? {
        val joined = texts.joinToString("\u0000")
        val cleaned = gate(joined, where) ?: return null
        return cleaned.split("\u0000")
    }
}

enum class ModerationNotice { PROFANITY_BLURRED, SLUR_WARNING, SEXUAL_BLOCKED, BANNED }

object ModerationBus {
    var notice by mutableStateOf<ModerationNotice?>(null)
    fun show(n: ModerationNotice) { notice = n }
}

/** Keeps strikes + ban flag in sync with Firestore. Place once at the app root. */
@Composable
fun ModerationWatcher(uid: String?) {
    DisposableEffect(uid) {
        var reg: ListenerRegistration? = null
        if (uid != null) {
            reg = FirebaseFirestore.getInstance().collection("users").document(uid)
                .addSnapshotListener { snap, _ ->
                    if (snap == null) return@addSnapshotListener
                    Moderation.slurStrikes = (snap.getLong("modSlurStrikes") ?: 0L).toInt()
                    Moderation.sexualStrikes = (snap.getLong("modSexualStrikes") ?: 0L).toInt()
                    Moderation.banned = snap.getBoolean("banned") == true
                    Moderation.bannedReason = snap.getString("bannedReason") ?: ""
                }
        } else {
            Moderation.slurStrikes = 0; Moderation.sexualStrikes = 0; Moderation.banned = false
        }
        onDispose {
            reg?.remove()
            // Signed out / switched account → don't carry strikes to the next user
            Moderation.slurStrikes = 0; Moderation.sexualStrikes = 0; Moderation.banned = false
        }
    }
}

/** Comic-style warning dialogs. Uses a Dialog so it shows above composers/sheets too. */
@Composable
fun ModerationNoticeHost() {
    val notice = ModerationBus.notice ?: return
    if (notice == ModerationNotice.BANNED) return   // the BannedScreen takes over
    val haptic = rememberComicHaptic()
    val pop = remember(notice) { Animatable(0.7f) }
    LaunchedEffect(notice) {
        haptic()
        Chiptune.play(if (notice == ModerationNotice.PROFANITY_BLURRED) Chiptune.Sfx.BLIP else Chiptune.Sfx.STAMP)
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
    }
    val (emoji, title, body, accent) = when (notice) {
        ModerationNotice.PROFANITY_BLURRED -> Quad("🙊", "WORDS BLURRED",
            "Your message had curse words, so we blurred them (▒▒▒). Please keep RetroHub friendly and don't use curse words.", CGreen)
        ModerationNotice.SLUR_WARNING -> Quad("⚠️", "FINAL WARNING",
            "Racist slurs and hate speech are not allowed. We blurred it. If this happens again your account will be banned.", CAcRed)
        ModerationNotice.SEXUAL_BLOCKED -> Quad("⛔", "MESSAGE NOT SENT",
            "Sexual content isn't allowed on RetroHub. This is your only warning — if you try again, your account will be banned.", CAcRed)
        ModerationNotice.BANNED -> Quad("", "", "", CAcRed)
    }
    Dialog(onDismissRequest = { ModerationBus.notice = null }) {
        Box(modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = (1f - pop.value) * -8f }) {
            Box(Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(16.dp)).background(accent))
            Column(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .border(3.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(emoji, fontSize = 40.sp)
                Text(title, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 26.sp, letterSpacing = 1.sp,
                    color = if (accent == CAcRed) CAcRed else ScrapbookDark))
                Text(body, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark,
                    textAlign = TextAlign.Center, lineHeight = 20.sp))
                RetroGlassButton(text = "GOT IT", onClick = { ModerationBus.notice = null }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private data class Quad(val a: String, val b: String, val c: String, val d: Color)

/** Full-screen lock for banned accounts. */
@Composable
fun BannedScreen(reason: String, onSignOut: () -> Unit) {
    var showAppeal by remember { mutableStateOf(false) }
    if (showAppeal) AppealBanDialog(onDismiss = { showAppeal = false })
    Box(modifier = Modifier.fillMaxSize().background(ScrapbookDark).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("GAME OVER", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 54.sp, letterSpacing = 3.sp, color = CAcRed))
            Text("ACCOUNT BANNED", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 24.sp, letterSpacing = 2.sp, color = Color.White))
            Text(
                "This account broke RetroHub's community rules" + (if (reason.isNotBlank()) ":\n$reason" else ".") +
                    "\n\nIf you think this is a mistake, send an appeal.",
                style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center, lineHeight = 20.sp)
            )
            Spacer(Modifier.height(8.dp))
            RetroGlassButton(text = "⚖️ APPEAL BAN", onClick = { showAppeal = true }, modifier = Modifier.fillMaxWidth())
            RetroGlassButton(text = "SIGN OUT", onClick = onSignOut, modifier = Modifier.fillMaxWidth())
        }
    }
}
