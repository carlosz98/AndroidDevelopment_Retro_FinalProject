package com.example.hubretro

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════════
// SUPPORT — Help Desk: tickets (bugs, reports, appeals…), community petitions, FAQ
//
// Firestore:
//   support_tickets/{id}  uid, username, category, subject, description, reportedHandle,
//                         priority, status (OPEN|IN_REVIEW|RESOLVED|CLOSED), staffReply,
//                         ticketNo, flagged{profanity,slur,sexual}, createdAt
//   petitions/{id}        title, description, category, creatorUid, creatorName,
//                         signatures, signers[], goal, status, createdAt
// ═══════════════════════════════════════════════════════════════════════════════

enum class TicketCategory(val label: String, val emoji: String, val blurb: String) {
    BUG("BUG / GLITCH", "🐛", "Something broke or looks wrong"),
    REPORT("REPORT PLAYER", "🚩", "Harassment, spam, cheating"),
    CONTENT("REPORT CONTENT", "🛡️", "A post, comment or article"),
    ACCOUNT("ACCOUNT", "🔑", "Login, profile, your data"),
    APPEAL("BAN APPEAL", "⚖️", "Think a ban was a mistake?"),
    OTHER("OTHER", "💡", "Ideas, questions, anything");

    companion object { fun from(s: String?) = values().firstOrNull { it.name == s } ?: OTHER }
}

enum class TicketStatus(val label: String, val step: Int) {
    OPEN("OPEN", 0), IN_REVIEW("IN REVIEW", 1), RESOLVED("RESOLVED", 2), CLOSED("CLOSED", 2);

    val color: Color get() = when (this) {
        OPEN -> CGreen; IN_REVIEW -> CGreenDeep; RESOLVED -> CGreenMint; CLOSED -> ScrapbookTextMuted
    }
    companion object { fun from(s: String?) = values().firstOrNull { it.name == s } ?: OPEN }
}

data class SupportTicket(
    val id: String = "",
    val ticketNo: String = "",
    val category: TicketCategory = TicketCategory.OTHER,
    val subject: String = "",
    val description: String = "",
    val reportedHandle: String = "",
    val priority: String = "NORMAL",
    val status: TicketStatus = TicketStatus.OPEN,
    val staffReply: String = "",
    val createdAt: Long = 0L
)

data class Petition(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "FEATURE",
    val creatorUid: String = "",
    val creatorName: String = "",
    val signatures: Int = 0,
    val signers: List<String> = emptyList(),
    val goal: Int = 50,
    val status: String = "OPEN",
    val createdAt: Long = 0L
)

private val petitionCategories = listOf("FEATURE", "COMMUNITY", "EVENTS", "CONTENT")
private const val PETITION_GOAL = 50

// ─── Data layer ───────────────────────────────────────────────────────────────

object SupportRepository {
    private val db get() = FirebaseFirestore.getInstance()

    private fun newTicketNo() = "RH-" + (100000..999999).random()

    /**
     * Ticket text is masked (▒) but NEVER gives strikes — people need to be able to quote
     * what they're reporting without getting banned. Staff see what kind of content was quoted.
     */
    suspend fun submitTicket(
        uid: String, username: String, category: TicketCategory, subject: String,
        description: String, reportedHandle: String, priority: String
    ): String? = try {
        val s = Moderation.scan(subject)
        val d = Moderation.scan(description)
        val h = Moderation.scan(reportedHandle)
        val no = newTicketNo()
        db.collection("support_tickets").add(
            hashMapOf(
                "uid" to uid,
                "username" to username,
                "category" to category.name,
                "subject" to s.cleaned.trim(),
                "description" to d.cleaned.trim(),
                "reportedHandle" to h.cleaned.trim(),
                "priority" to priority,
                "status" to TicketStatus.OPEN.name,
                "staffReply" to "",
                "ticketNo" to no,
                "flagged" to mapOf(
                    "profanity" to (s.profanity || d.profanity || h.profanity),
                    "slur" to (s.slur || d.slur || h.slur),
                    "sexual" to (s.sexual || d.sexual || h.sexual)
                ),
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        no
    } catch (e: Exception) { null }

    fun listenMyTickets(uid: String, onChange: (List<SupportTicket>) -> Unit): ListenerRegistration =
        db.collection("support_tickets").whereEqualTo("uid", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                onChange(snap.documents.map { d ->
                    SupportTicket(
                        id = d.id,
                        ticketNo = d.getString("ticketNo") ?: "",
                        category = TicketCategory.from(d.getString("category")),
                        subject = d.getString("subject") ?: "",
                        description = d.getString("description") ?: "",
                        reportedHandle = d.getString("reportedHandle") ?: "",
                        priority = d.getString("priority") ?: "NORMAL",
                        status = TicketStatus.from(d.getString("status")),
                        staffReply = d.getString("staffReply") ?: "",
                        createdAt = d.getLong("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.createdAt })
            }

    fun listenPetitions(onChange: (List<Petition>) -> Unit): ListenerRegistration =
        db.collection("petitions").limit(100)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                onChange(snap.documents.map { d ->
                    Petition(
                        id = d.id,
                        title = d.getString("title") ?: "",
                        description = d.getString("description") ?: "",
                        category = d.getString("category") ?: "FEATURE",
                        creatorUid = d.getString("creatorUid") ?: "",
                        creatorName = d.getString("creatorName") ?: "",
                        signatures = (d.getLong("signatures") ?: 0L).toInt(),
                        signers = (d.get("signers") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        goal = (d.getLong("goal") ?: PETITION_GOAL.toLong()).toInt(),
                        status = d.getString("status") ?: "OPEN",
                        createdAt = d.getLong("createdAt") ?: 0L
                    )
                })
            }

    /** Returns true if this is a new signature (transaction = no double signing). */
    suspend fun sign(petitionId: String, uid: String): Boolean = try {
        val ref = db.collection("petitions").document(petitionId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val signers = (snap.get("signers") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (uid in signers) false
            else {
                tx.update(ref, mapOf("signers" to FieldValue.arrayUnion(uid), "signatures" to FieldValue.increment(1)))
                true
            }
        }.await()
    } catch (e: Exception) { false }

    suspend fun createPetition(title: String, description: String, category: String, uid: String, name: String): Boolean = try {
        db.collection("petitions").add(
            hashMapOf(
                "title" to title.trim(),
                "description" to description.trim(),
                "category" to category,
                "creatorUid" to uid,
                "creatorName" to name,
                "signatures" to 1,
                "signers" to listOf(uid),       // creator signs automatically
                "goal" to PETITION_GOAL,
                "status" to "OPEN",
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        true
    } catch (e: Exception) { false }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun SupportScreen(authViewModel: AuthViewModel) {
    val profile by authViewModel.userProfile.collectAsState()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val username = profile?.username?.ifBlank { null } ?: "Player"

    var tab by rememberSaveable { mutableIntStateOf(0) }
    val burst = rememberBurstState()

    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var ticketsLoaded by remember { mutableStateOf(false) }
    var petitions by remember { mutableStateOf<List<Petition>>(emptyList()) }
    var petitionsLoaded by remember { mutableStateOf(false) }

    DisposableEffect(uid) {
        val a = if (uid.isNotBlank()) SupportRepository.listenMyTickets(uid) { tickets = it; ticketsLoaded = true } else null
        val b = SupportRepository.listenPetitions { petitions = it; petitionsLoaded = true }
        onDispose { a?.remove(); b.remove() }
    }

    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneDots(modifier = Modifier.fillMaxSize(), color = CGreenDeep.copy(alpha = 0.08f))

        Column(modifier = Modifier.fillMaxSize()) {
            HelpDeskHero(
                openCount = tickets.count { it.status == TicketStatus.OPEN || it.status == TicketStatus.IN_REVIEW },
                petitionCount = petitions.count { it.status == "OPEN" },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            )
            InkTabRow(
                tabs = listOf("NEW TICKET", "MY TICKETS", "PETITIONS", "FAQ"),
                selectedIndex = tab,
                onSelect = { tab = it; Chiptune.play(Chiptune.Sfx.BLIP) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(spring(dampingRatio = 0.8f, stiffness = 320f)) { w -> dir * w / 2 } + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally(tween(200)) { w -> -dir * w / 3 } + fadeOut(tween(150)))
                },
                modifier = Modifier.weight(1f),
                label = "supportTabs"
            ) { t ->
                when (t) {
                    0 -> NewTicketTab(uid = uid, username = username, burst = burst, onViewTickets = { tab = 1 })
                    1 -> MyTicketsTab(tickets = tickets, loaded = ticketsLoaded || uid.isBlank(), onNewTicket = { tab = 0 })
                    2 -> PetitionsTab(petitions = petitions, loaded = petitionsLoaded, uid = uid, username = username, burst = burst)
                    else -> FaqTab(onContact = { tab = 0 })
                }
            }
        }
        ComicBurst(state = burst, modifier = Modifier.align(Alignment.Center))
    }
}

// ─── Hero ─────────────────────────────────────────────────────────────────────

@Composable
private fun HelpDeskHero(openCount: Int, petitionCount: Int, modifier: Modifier = Modifier) {
    val bob by rememberGlowRange(-3f, 3f)
    Box(modifier = modifier.fillMaxWidth().dealIn(0, 1)) {
        Box(Modifier.matchParentSize().offset(4.dp, 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(58.dp).graphicsLayer { translationY = bob * density }
                    .clip(CircleShape).background(CGreen).border(2.5.dp, ScrapbookDark, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("🛟", fontSize = 28.sp) }
            Column(modifier = Modifier.weight(1f)) {
                Text("HELP DESK", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 24.sp, letterSpacing = 1.sp, color = ScrapbookDark))
                ArcadeBlinkText("● SUPPORT TEAM ONLINE", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 12.sp, letterSpacing = 1.sp, color = CGreenDeep))
                Text("Report bugs, players or content, appeal a ban, or rally the community behind an idea.",
                    style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookTextMuted, lineHeight = 16.sp))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HeroStat(openCount.toString(), "OPEN")
                HeroStat(petitionCount.toString(), "PETITIONS")
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ComicGlassBg)
            .border(1.5.dp, ScrapbookDark, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RollingCounterText(value, TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, color = CGreenDeep))
        Text(label, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 9.sp, letterSpacing = 1.sp, color = ScrapbookTextMuted))
    }
}

// ─── Tab 1: New ticket ────────────────────────────────────────────────────────

@Composable
private fun NewTicketTab(uid: String, username: String, burst: BurstState, onViewTickets: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptic = rememberComicHaptic()
    val coin = rememberCoinDrop()
    val shake = remember { Animatable(0f) }

    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var subject by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var handle by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf("NORMAL") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var receipt by rememberSaveable { mutableStateOf<String?>(null) }

    val cat = category?.let { TicketCategory.from(it) }
    val needsHandle = cat == TicketCategory.REPORT || cat == TicketCategory.CONTENT

    fun reset() { category = null; subject = ""; description = ""; handle = ""; priority = "NORMAL"; error = null; receipt = null }

    fun submit() {
        if (sending || cat == null) return
        error = when {
            uid.isBlank() -> "You need to be signed in"
            subject.trim().length < 4 -> "Give your ticket a short title (4+ characters)"
            description.trim().length < 15 -> "Tell us a bit more (15+ characters)"
            needsHandle && handle.isBlank() -> "Who or what are you reporting?"
            else -> null
        }
        if (error != null) {
            haptic()
            scope.launch { for (x in listOf(14f, -12f, 9f, -6f, 3f, 0f)) shake.animateTo(x, tween(45)) }
            return
        }
        coin.fire()
        sending = true
        scope.launch {
            val no = SupportRepository.submitTicket(uid, username, cat, subject, description, handle, priority)
            sending = false
            if (no == null) {
                error = "Couldn't send — check your connection and try again"
            } else {
                receipt = no
                burst.fire("TICKET SENT!", CGreenMint)
                Chiptune.play(Chiptune.Sfx.FANFARE)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val r = receipt
        if (r != null) {
            item(key = "receipt") {
                TicketReceipt(
                    ticketNo = r,
                    category = cat ?: TicketCategory.OTHER,
                    subject = subject,
                    onViewTickets = { reset(); onViewTickets() },
                    onAnother = { reset() }
                )
            }
            return@LazyColumn
        }

        item(key = "pick") { SectionLabel("1 · CHOOSE YOUR QUEST") }
        item(key = "grid") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TicketCategory.values().toList().chunked(2).forEachIndexed { row, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEachIndexed { col, c ->
                            Box(modifier = Modifier.weight(1f).jumpIn(row * 2 + col)) {
                                CategoryCartridge(c, selected = cat == c) {
                                    category = c.name; error = null
                                    Chiptune.play(Chiptune.Sfx.POP)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (cat != null) {
            item(key = "form_label") { SectionLabel("2 · FILL IN THE DETAILS") }
            item(key = "form") {
                Box(modifier = Modifier.graphicsLayer { translationX = shake.value * density }.dealIn(0, 2)) {
                    Box(Modifier.matchParentSize().offset(4.dp, 4.dp).clip(RoundedCornerShape(16.dp)).background(CGreen))
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(cat.emoji, fontSize = 22.sp)
                            Text(cat.label, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 20.sp, letterSpacing = 1.sp, color = ScrapbookDark))
                        }
                        if (cat == TicketCategory.APPEAL) {
                            HintBubble("Tell us what happened and why you think the ban was a mistake. A real person reads every appeal.")
                        }
                        if (needsHandle) {
                            SupportField(handle, { handle = it.take(60) },
                                label = if (cat == TicketCategory.REPORT) "PLAYER @HANDLE" else "WHERE IS IT? (post, article, @author…)",
                                placeholder = if (cat == TicketCategory.REPORT) "@username" else "e.g. comment on \"Best SNES RPGs\"")
                        }
                        SupportField(subject, { subject = it.take(80) }, label = "TITLE", placeholder = "Short summary", maxChars = 80)
                        SupportField(description, { description = it.take(1000) }, label = "WHAT HAPPENED?",
                            placeholder = "Steps, screen, what you expected…", singleLine = false, maxChars = 1000)

                        Text("DIFFICULTY", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, letterSpacing = 1.sp, color = ScrapbookTextMuted))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("EASY" to "Low", "NORMAL" to "Medium", "HARD" to "Urgent").forEach { (p, sub) ->
                                PriorityChip(p, sub, selected = priority == p, modifier = Modifier.weight(1f)) {
                                    priority = p; Chiptune.play(Chiptune.Sfx.BLIP)
                                }
                            }
                        }

                        AnimatedVisibility(visible = error != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Text("⚠ " + (error ?: ""), style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = CAcRed))
                        }

                        Box(contentAlignment = Alignment.TopCenter) {
                            RetroGlassButton(
                                text = if (sending) "SENDING…" else "INSERT COIN · SEND TICKET",
                                onClick = { submit() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            CoinDrop(state = coin)
                        }
                        Text("Be honest and specific. False reports can lead to action on your own account.",
                            style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        } else {
            item(key = "hint") {
                HintBubble("Pick a category above to start. Every ticket gets a number so you can track it in MY TICKETS.")
            }
        }
    }
}

@Composable
private fun CategoryCartridge(c: TicketCategory, selected: Boolean, onClick: () -> Unit) {
    val lift by animateFloatAsState(if (selected) -6f else 0f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "cartLift")
    val tilt by animateFloatAsState(if (selected) -2f else 0f, spring(dampingRatio = 0.5f), label = "cartTilt")
    Box(modifier = Modifier.fillMaxWidth().graphicsLayer { translationY = lift * density; rotationZ = tilt }) {
        Box(Modifier.matchParentSize().offset(3.dp, 3.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark))
        Column(
            modifier = Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(12.dp))
                .background(if (selected) CGreen else Color.White)
                .border(if (selected) 3.dp else 2.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
        ) {
            // cartridge "label notch" strip
            Row(
                modifier = Modifier.fillMaxWidth().height(10.dp).background(if (selected) CGreenDeep else ComicGlassBg),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(5) { Box(Modifier.width(3.dp).fillMaxHeight().background(ScrapbookDark.copy(alpha = 0.25f))) }
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(c.emoji, fontSize = 20.sp)
                    Text(c.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontFamily = BangersFontFamily, fontSize = 15.sp, letterSpacing = 0.5.sp,
                            color = if (selected) Color.White else ScrapbookDark))
                }
                Spacer(Modifier.height(2.dp))
                Text(c.blurb, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, lineHeight = 14.sp,
                        color = if (selected) Color.White.copy(alpha = 0.92f) else ScrapbookTextMuted))
            }
        }
    }
}

@Composable
private fun PriorityChip(label: String, sub: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.05f else 1f, spring(dampingRatio = 0.45f), label = "prioScale")
    Column(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) CGreen else ComicGlassBg)
            .border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 14.sp, letterSpacing = 1.sp,
            color = if (selected) Color.White else ScrapbookDark))
        Text(sub, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 10.sp,
            color = if (selected) Color.White.copy(alpha = 0.9f) else ScrapbookTextMuted))
    }
}

/** Arcade prize-ticket that "prints" out of a slot after submitting. */
@Composable
private fun TicketReceipt(
    ticketNo: String,
    category: TicketCategory,
    subject: String,
    onViewTickets: () -> Unit,
    onAnother: () -> Unit
) {
    val reveal = remember(ticketNo) { Animatable(0f) }
    LaunchedEffect(ticketNo) {
        delay(150)
        repeat(4) { i ->
            reveal.animateTo((i + 1) / 4f, tween(220, easing = LinearEasing))
            Chiptune.play(Chiptune.Sfx.BLIP)
        }
    }
    val date = remember { SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date()) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // printer slot
        Box(
            Modifier.fillMaxWidth(0.9f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(ScrapbookDark)
        )
        Box(
            modifier = Modifier.fillMaxWidth(0.82f)
                .offset(y = (-4).dp)
                .drawWithContent { clipRect(bottom = size.height * reveal.value) { this@drawWithContent.drawContent() } }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White)
                    .border(2.dp, ScrapbookDark).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("★ RETROHUB SUPPORT ★", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 14.sp, letterSpacing = 2.sp, color = CGreenDeep))
                DashedDivider()
                Text("TICKET", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 12.sp, letterSpacing = 3.sp, color = ScrapbookTextMuted))
                Text(ticketNo, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 36.sp, letterSpacing = 2.sp, color = ScrapbookDark))
                Text("${category.emoji}  ${category.label}", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 15.sp, color = CGreenDeep))
                Text(subject, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark))
                DashedDivider()
                Text(date, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted))
                Text("Status: OPEN · we'll reply in MY TICKETS", textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookDark))
                // barcode
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
                    ticketNo.filter { it.isDigit() }.forEach { d ->
                        val w = (1 + (d - '0') % 3).dp
                        Box(Modifier.width(w).height(26.dp).background(ScrapbookDark))
                        Box(Modifier.width(1.dp).height(26.dp).background(ScrapbookDark))
                    }
                }
            }
        }
        AnimatedVisibility(visible = reveal.value >= 1f, enter = fadeIn() + expandVertically()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RetroGlassButton(text = "VIEW MY TICKETS", onClick = onViewTickets, modifier = Modifier.fillMaxWidth())
                Text("+ SEND ANOTHER", modifier = Modifier.fillMaxWidth().clickable(onClick = onAnother).padding(8.dp),
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = BangersFontFamily, fontSize = 14.sp, letterSpacing = 1.sp, color = CGreenDeep))
            }
        }
    }
}

@Composable
private fun DashedDivider() {
    Canvas(Modifier.fillMaxWidth().height(2.dp)) {
        drawLine(
            ScrapbookDark.copy(alpha = 0.4f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
            strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )
    }
}

// ─── Tab 2: My tickets ────────────────────────────────────────────────────────

@Composable
private fun MyTicketsTab(tickets: List<SupportTicket>, loaded: Boolean, onNewTicket: () -> Unit) {
    when {
        !loaded -> NowLoadingIndicator(label = "LOADING TICKETS")
        tickets.isEmpty() -> NoSaveDataState(
            subtitle = "You haven't sent any tickets yet. Hopefully that means everything's working!",
            title = "NO TICKETS",
            actionText = "NEW TICKET",
            onAction = onNewTicket
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(tickets, key = { _, t -> "t_" + t.id }) { i, t ->
                Box(Modifier.dealIn(i.coerceAtMost(6) * 60, i)) { TicketCard(t) }
            }
        }
    }
}

@Composable
private fun TicketCard(t: SupportTicket) {
    var expanded by remember { mutableStateOf(false) }
    val date = remember(t.createdAt) {
        if (t.createdAt == 0L) "" else SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(t.createdAt))
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().offset(4.dp, 4.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
                .border(2.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .clickable { expanded = !expanded; Chiptune.play(Chiptune.Sfx.BLIP) }
                .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 400f))
        ) {
            Box(Modifier.width(8.dp).fillMaxHeight().background(t.status.color))
            Column(modifier = Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${t.category.emoji} ${t.category.label}", modifier = Modifier.weight(1f),
                        style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, letterSpacing = 1.sp, color = CGreenDeep))
                    Text("#${t.ticketNo} · $date", style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.subject, modifier = Modifier.weight(1f), maxLines = if (expanded) 4 else 1, overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontFamily = BangersFontFamily, fontSize = 19.sp, color = ScrapbookDark))
                    StatusStamp(t.status, key = t.id + t.status.name)
                }
                Text(t.description, maxLines = if (expanded) 40 else 2, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = ScrapbookDark.copy(alpha = 0.8f)))
                if (expanded && t.reportedHandle.isNotBlank()) {
                    Text("Reported: ${t.reportedHandle}", style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookTextMuted))
                }
                QuestProgress(t.status)
                if (t.staffReply.isNotBlank()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ComicGlassBg)
                            .border(1.5.dp, CGreenDeep, RoundedCornerShape(12.dp)).padding(10.dp)
                    ) {
                        Text("🛟 RETROHUB TEAM", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, letterSpacing = 1.sp, color = CGreenDeep))
                        Text(t.staffReply, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = ScrapbookDark))
                    }
                }
                if (!expanded) {
                    Text("TAP FOR DETAILS", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp, letterSpacing = 1.sp, color = ScrapbookTextMuted.copy(alpha = 0.7f)))
                }
            }
        }
    }
}

@Composable
private fun StatusStamp(status: TicketStatus, key: Any) {
    Box(
        modifier = Modifier.padding(start = 8.dp).stampIn(key).rotate(-8f)
            .border(2.dp, status.color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(status.label, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, letterSpacing = 1.sp, color = status.color))
    }
}

/** SENT ── IN REVIEW ── RESOLVED, like a world-map path. */
@Composable
private fun QuestProgress(status: TicketStatus) {
    val fill by animateFloatAsState(status.step / 2f, tween(800, easing = FastOutSlowInEasing), label = "questFill")
    val pulse by rememberGlowRange(1f, 1.15f)
    val labels = listOf("SENT", "IN REVIEW", if (status == TicketStatus.CLOSED) "CLOSED" else "RESOLVED")
    Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 9.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookDark.copy(alpha = 0.1f)))
            Box(Modifier.fillMaxWidth().padding(horizontal = 9.dp)) {
                Box(Modifier.fillMaxWidth(fill).height(4.dp).clip(RoundedCornerShape(2.dp)).background(CGreen))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(3) { i ->
                    val done = i <= status.step
                    val current = i == status.step && status != TicketStatus.RESOLVED && status != TicketStatus.CLOSED
                    Box(
                        modifier = Modifier.size(18.dp)
                            .graphicsLayer { val s = if (current) pulse else 1f; scaleX = s; scaleY = s }
                            .clip(CircleShape).background(if (done) CGreen else Color.White)
                            .border(2.dp, ScrapbookDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) Text("✓", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp, color = Color.White))
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, l ->
                Text(l, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp, letterSpacing = 0.5.sp,
                    color = if (i <= status.step) CGreenDeep else ScrapbookTextMuted))
            }
        }
    }
}

// ─── Tab 3: Petitions ─────────────────────────────────────────────────────────

@Composable
private fun PetitionsTab(petitions: List<Petition>, loaded: Boolean, uid: String, username: String, burst: BurstState) {
    var sort by rememberSaveable { mutableStateOf("HOT") }
    var showCreate by remember { mutableStateOf(false) }
    val sorted = remember(petitions, sort) {
        if (sort == "HOT") petitions.sortedWith(compareByDescending<Petition> { it.signatures }.thenByDescending { it.createdAt })
        else petitions.sortedByDescending { it.createdAt }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "p_head") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HintBubble("Want a feature, event or change? Start a petition. At $PETITION_GOAL signatures the team reviews it and answers publicly.")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HOT" to "🔥 HOT", "NEW" to "✨ NEW").forEachIndexed { i, (k, label) ->
                        Box(Modifier.jumpIn(i)) {
                            PriorityChip(label, if (k == "HOT") "Most signed" else "Latest", selected = sort == k,
                                modifier = Modifier.width(96.dp)) { sort = k; Chiptune.play(Chiptune.Sfx.BLIP) }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.jumpIn(2)) {
                        RetroGlassButton(text = "+ START ONE", onClick = { showCreate = true; Chiptune.play(Chiptune.Sfx.POP) })
                    }
                }
            }
        }
        when {
            !loaded -> item(key = "p_load") { NowLoadingIndicator(label = "COUNTING VOTES") }
            sorted.isEmpty() -> item(key = "p_empty") {
                NoSaveDataState(subtitle = "No petitions yet. Be the first to rally the community!", title = "NO PETITIONS",
                    actionText = "START A PETITION", onAction = { showCreate = true })
            }
            else -> itemsIndexed(sorted, key = { _, p -> "p_" + p.id }) { i, p ->
                Box(Modifier.dealIn(i.coerceAtMost(6) * 60, i)) {
                    PetitionCard(p, uid = uid, burst = burst)
                }
            }
        }
    }

    if (showCreate) {
        CreatePetitionDialog(uid = uid, username = username, onDismiss = { showCreate = false }, onCreated = {
            showCreate = false
            burst.fire("PETITION LIVE!", CGreenMint)
            Chiptune.play(Chiptune.Sfx.FANFARE)
        })
    }
}

@Composable
private fun PetitionCard(p: Petition, uid: String, burst: BurstState) {
    val scope = rememberCoroutineScope()
    val haptic = rememberComicHaptic()
    var signing by remember { mutableStateOf(false) }
    val signed = uid.isNotBlank() && uid in p.signers
    val goalReached = p.signatures >= p.goal
    val fill by animateFloatAsState((p.signatures.toFloat() / p.goal.coerceAtLeast(1)).coerceIn(0f, 1f),
        tween(900, easing = FastOutSlowInEasing), label = "petitionFill")
    val btnScale = remember { Animatable(1f) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().offset(4.dp, 4.dp).clip(RoundedCornerShape(14.dp)).background(if (goalReached) CGreen else ScrapbookDark))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
                .border(2.dp, ScrapbookDark, RoundedCornerShape(14.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(6.dp)).background(ComicGlassBg)
                        .border(1.5.dp, ScrapbookDark, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(p.category, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 11.sp, letterSpacing = 1.sp, color = CGreenDeep))
                }
                Spacer(Modifier.width(8.dp))
                Text("by ${p.creatorName}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted))
                if (goalReached) {
                    Box(
                        Modifier.stampIn("goal_" + p.id).rotate(-8f).border(2.dp, CGreenDeep, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(if (p.status == "OPEN") "GOAL REACHED" else p.status,
                            style = TextStyle(fontFamily = BangersFontFamily, fontSize = 12.sp, letterSpacing = 1.sp, color = CGreenDeep))
                    }
                }
            }
            Text(p.title, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 20.sp, lineHeight = 22.sp, color = ScrapbookDark))
            Text(p.description, maxLines = 4, overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = ScrapbookDark.copy(alpha = 0.8f)))

            // EXP-style signature bar
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("EXP", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, color = CGreenDeep))
                Box(
                    modifier = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp))
                        .background(ScrapbookDark.copy(alpha = 0.08f)).border(1.5.dp, ScrapbookDark, RoundedCornerShape(7.dp))
                ) {
                    Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(CGreen))
                    // segment ticks
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        repeat(9) { Box(Modifier.width(1.5.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.6f))) }
                    }
                }
                RollingCounterText("${p.signatures}/${p.goal}", TextStyle(fontFamily = BangersFontFamily, fontSize = 14.sp, color = ScrapbookDark))
            }

            Box(
                modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = btnScale.value; scaleY = btnScale.value }
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (signed) ComicGlassBg else CGreen)
                    .border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                    .clickable(enabled = !signed && !signing && uid.isNotBlank()) {
                        signing = true
                        haptic()
                        scope.launch {
                            btnScale.animateTo(0.9f, tween(80))
                            btnScale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 600f))
                        }
                        scope.launch {
                            val ok = SupportRepository.sign(p.id, uid)
                            signing = false
                            if (ok) {
                                burst.fire(if (p.signatures + 1 >= p.goal) "GOAL!" else "SIGNED!", CGreenMint)
                                Chiptune.play(if (p.signatures + 1 >= p.goal) Chiptune.Sfx.FANFARE else Chiptune.Sfx.POWER_UP)
                            }
                        }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when { signed -> "✓ YOU SIGNED THIS"; signing -> "SIGNING…"; else -> "✍️ SIGN PETITION" },
                    style = TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, letterSpacing = 1.sp,
                        color = if (signed) CGreenDeep else Color.White)
                )
            }
        }
    }
}

@Composable
private fun CreatePetitionDialog(uid: String, username: String, onDismiss: () -> Unit, onCreated: () -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(petitionCategories.first()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Box(modifier = Modifier.comicPop()) {
            Box(Modifier.matchParentSize().offset(6.dp, 6.dp).clip(RoundedCornerShape(16.dp)).background(CGreen))
            Column(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .border(3.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("📜 START A PETITION", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 24.sp, letterSpacing = 1.sp, color = ScrapbookDark))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    petitionCategories.forEach { c ->
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (category == c) CGreen else ComicGlassBg)
                                .border(1.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                                .clickable { category = c; Chiptune.play(Chiptune.Sfx.BLIP) }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(c, maxLines = 1, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp,
                                color = if (category == c) Color.White else ScrapbookDark))
                        }
                    }
                }
                SupportField(title, { title = it.take(70) }, label = "PETITION TITLE", placeholder = "e.g. Add a monthly speedrun event", maxChars = 70)
                SupportField(desc, { desc = it.take(600) }, label = "WHY DOES IT MATTER?", placeholder = "Convince the community…",
                    singleLine = false, maxChars = 600)
                if (error != null) Text("⚠ $error", style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = CAcRed))
                RetroGlassButton(
                    text = if (busy) "POSTING…" else "LAUNCH PETITION",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (busy) return@RetroGlassButton
                        error = when {
                            uid.isBlank() -> "You need to be signed in"
                            title.trim().length < 6 -> "Title needs 6+ characters"
                            desc.trim().length < 20 -> "Explain it a little more (20+ characters)"
                            else -> null
                        }
                        if (error != null) return@RetroGlassButton
                        // Public content → full moderation (blur / block / strikes)
                        val safe = Moderation.gateAll(title, desc, where = "petition") ?: run { onDismiss(); return@RetroGlassButton }
                        busy = true
                        scope.launch {
                            val ok = SupportRepository.createPetition(safe[0], safe[1], category, uid, username)
                            busy = false
                            if (ok) onCreated() else error = "Couldn't post — try again"
                        }
                    }
                )
                Text("CANCEL", modifier = Modifier.fillMaxWidth().clickable { if (!busy) onDismiss() }.padding(6.dp),
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = BangersFontFamily, fontSize = 14.sp, letterSpacing = 1.sp, color = ScrapbookTextMuted))
            }
        }
    }
}

// ─── Tab 4: FAQ + rules ───────────────────────────────────────────────────────

private val supportFaq = listOf(
    "How long until someone answers my ticket?" to "Most tickets get a reply within 48 hours. Urgent (HARD) tickets about safety are looked at first. You'll see the reply right on the ticket in MY TICKETS.",
    "I was banned. What can I do?" to "Open a BAN APPEAL ticket (or use the APPEAL button on the ban screen). Explain what happened. A real person reviews every appeal.",
    "How do I report a player?" to "Use REPORT PLAYER and include their @handle and what happened. Reports are private — the player never sees who reported them.",
    "Why were some of my words blurred?" to "RetroHub blurs curse words automatically (▒▒▒). It's not a strike — just keep it friendly.",
    "What happens when a petition hits its goal?" to "At $PETITION_GOAL signatures the team reviews it and marks it ACCEPTED, PLANNED or DECLINED with an answer for everyone.",
    "My game or album search shows nothing." to "Search needs an internet connection. If it keeps failing, send a BUG / GLITCH ticket with what you searched for."
)

@Composable
private fun FaqTab(onContact: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "rules") { CommunityRulesCard() }
        item(key = "faq_label") { SectionLabel("FREQUENTLY ASKED") }
        itemsIndexed(supportFaq, key = { i, _ -> "faq_$i" }) { i, (q, a) ->
            Box(Modifier.jumpIn(i)) { FaqCard(q, a) }
        }
        item(key = "contact") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Still stuck?", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookDark))
                Spacer(Modifier.height(6.dp))
                RetroGlassButton(text = "OPEN A TICKET", onClick = onContact, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CommunityRulesCard() {
    Box(modifier = Modifier.fillMaxWidth().dealIn(0, 3)) {
        Box(Modifier.matchParentSize().offset(4.dp, 4.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📜 HOUSE RULES", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 22.sp, letterSpacing = 1.sp, color = ScrapbookDark))
            RuleRow("🙊", "Curse words", "Blurred automatically. Keep it friendly.", CGreen)
            RuleRow("⚠️", "Racist slurs / hate", "Blurred + final warning. Second time = ban.", CAcRed)
            RuleRow("⛔", "Sexual content", "Blocked + warning. Trying again = ban.", CAcRed)
            RuleRow("🤝", "Respect", "No harassment, spam or cheating. Report it, don't fight it.", CGreenDeep)
        }
    }
}

@Composable
private fun RuleRow(emoji: String, title: String, body: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(ComicGlassBg).border(2.dp, accent, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 16.sp) }
        Column {
            Text(title, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 15.sp, letterSpacing = 0.5.sp, color = ScrapbookDark))
            Text(body, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = ScrapbookTextMuted))
        }
    }
}

@Composable
private fun FaqCard(q: String, a: String) {
    var open by remember { mutableStateOf(false) }
    val arrow by animateFloatAsState(if (open) 90f else 0f, spring(dampingRatio = 0.6f), label = "faqArrow")
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
            .border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp))
            .clickable { open = !open; Chiptune.play(Chiptune.Sfx.BLIP) }
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 400f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("▶", modifier = Modifier.rotate(arrow), style = TextStyle(fontSize = 12.sp, color = CGreenDeep))
            Spacer(Modifier.width(8.dp))
            Text(q, modifier = Modifier.weight(1f), style = TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, color = ScrapbookDark))
        }
        if (open) {
            Spacer(Modifier.height(6.dp))
            Text(a, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = ScrapbookDark.copy(alpha = 0.8f)))
        }
    }
}

// ─── Ban appeal (from the BannedScreen) ───────────────────────────────────────

@Composable
fun AppealBanDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var sentNo by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Column(
            modifier = Modifier.comicPop().clip(RoundedCornerShape(16.dp)).background(Color.White)
                .border(3.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val no = sentNo
            if (no != null) {
                Text("⚖️", fontSize = 40.sp)
                Text("APPEAL SENT", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 26.sp, color = CGreenDeep))
                Text("Ticket #$no. A person from the team will review it. You can check back by signing in again later.",
                    textAlign = TextAlign.Center, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark))
                RetroGlassButton(text = "OK", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            } else {
                Text("⚖️ APPEAL BAN", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 24.sp, color = ScrapbookDark))
                SupportField(text, { text = it.take(1000) }, label = "WHY SHOULD WE REVIEW IT?",
                    placeholder = "Explain what happened…", singleLine = false, maxChars = 1000)
                if (error != null) Text("⚠ $error", style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = CAcRed))
                RetroGlassButton(
                    text = if (busy) "SENDING…" else "SEND APPEAL",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (busy) return@RetroGlassButton
                        if (text.trim().length < 15) { error = "Please write at least 15 characters"; return@RetroGlassButton }
                        busy = true
                        scope.launch {
                            val r = SupportRepository.submitTicket(uid, "banned user",
                                TicketCategory.APPEAL, "Ban appeal", text, "", "HARD")
                            busy = false
                            if (r != null) { sentNo = r; Chiptune.play(Chiptune.Sfx.POWER_UP) } else error = "Couldn't send — try again"
                        }
                    }
                )
            }
        }
    }
}

// ─── Shared bits ──────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, modifier = Modifier.padding(top = 4.dp),
        style = TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, letterSpacing = 1.5.sp, color = CGreenDeep))
}

@Composable
private fun HintBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f))
            .border(BorderStroke(1.5.dp, CGreenDeep), RoundedCornerShape(12.dp)).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("💡", fontSize = 16.sp)
        Text(text, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, lineHeight = 17.sp, color = ScrapbookDark))
    }
}

@Composable
private fun SupportField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean = true,
    maxChars: Int? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f),
                style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, letterSpacing = 1.sp, color = ScrapbookTextMuted))
            if (maxChars != null) {
                Text("${value.length}/$maxChars", style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 10.sp,
                    color = if (value.length >= maxChars) CAcRed else ScrapbookTextMuted))
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 4,
            maxLines = if (singleLine) 1 else 8,
            placeholder = { Text(placeholder, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted.copy(alpha = 0.7f))) },
            textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CGreen,
                unfocusedBorderColor = ScrapbookDark,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = ComicGlassBg,
                cursorColor = CGreenDeep
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
