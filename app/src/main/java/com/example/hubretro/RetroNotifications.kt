package com.example.hubretro

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════════════════════════════════════════════
// NOTIFICATIONS
//
//  Two delivery paths:
//   1. RetroSyncWorker (on device, every 6h) — new releases / DLC on the calendar,
//      "out today" (+ your wishlist), fresh news, new community articles, ticket
//      replies, petition goals, and the WEEKLY UPDATE. Works with no server.
//   2. Firebase Cloud Messaging — instant pushes sent by Cloud Functions
//      (functions/index.js): chat messages, follows/comments/reactions,
//      support replies, petition goals, weekly topic push.
//  Every notification opens the right page via the "nav" extra.
// ═══════════════════════════════════════════════════════════════════════════════

object RetroNotify {
    private const val PREFS = "notif_prefs"
    const val EXTRA_NAV = "nav"
    const val EXTRA_OPEN_WEEKLY = "open_weekly"

    enum class Channel(val id: String, val title: String, val desc: String, val emoji: String,
                       val importance: Int, val topic: String?) {
        RELEASES("releases", "Release calendar", "New games, DLC & expansions, and what's out today", "📅", NotificationManager.IMPORTANCE_DEFAULT, "releases"),
        NEWS("news", "Gaming news", "Fresh stories from retro & gaming sites", "📰", NotificationManager.IMPORTANCE_LOW, "news"),
        COMMUNITY("community", "Community", "Messages, follows, comments & new community articles", "💬", NotificationManager.IMPORTANCE_HIGH, "community"),
        SUPPORT("support", "Support", "Replies to your tickets and petition milestones", "🛟", NotificationManager.IMPORTANCE_HIGH, null),
        WEEKLY("weekly", "Weekly update", "Your weekly RetroHub recap", "🗓️", NotificationManager.IMPORTANCE_DEFAULT, "weekly");

        companion object { fun from(id: String?) = values().firstOrNull { it.id == id } ?: COMMUNITY }
    }

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        Channel.values().forEach { c ->
            nm.createNotificationChannel(NotificationChannel(c.id, c.title, c.importance).apply { description = c.desc })
        }
    }

    fun isEnabled(context: Context, channel: Channel): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(channel.id, true)

    fun setEnabled(context: Context, channel: Channel, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(channel.id, on).apply()
        RetroPush.syncTopics(context)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(
        context: Context,
        channel: Channel,
        key: String,
        title: String,
        body: String,
        nav: String? = null,
        openWeekly: Boolean = false,
        bigText: String? = null
    ) {
        if (!isEnabled(context, channel) || !hasPermission(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (nav != null) putExtra(EXTRA_NAV, nav)
            if (openWeekly) putExtra(EXTRA_OPEN_WEEKLY, true)
        }
        val pi = PendingIntent.getActivity(
            context, key.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_stat_retrohub)
            .setColor(CGreen.toArgb())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText ?: body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(if (channel.importance >= NotificationManager.IMPORTANCE_HIGH) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(key.hashCode(), n)
        } catch (e: SecurityException) {
            Log.w("RetroNotify", "Notification permission missing")
        }
    }
}

/** Handles taps on notifications (cold start or app already open). */
fun handleNotificationIntent(intent: Intent?) {
    intent ?: return
    intent.getStringExtra(RetroNotify.EXTRA_NAV)?.let { AppNavBus.go(it) }
    if (intent.getBooleanExtra(RetroNotify.EXTRA_OPEN_WEEKLY, false)) WeeklyDigestBus.open = true
    intent.removeExtra(RetroNotify.EXTRA_NAV)
    intent.removeExtra(RetroNotify.EXTRA_OPEN_WEEKLY)
}

// ─── FCM: token + topics ──────────────────────────────────────────────────────

object RetroPush {
    /** Call when a user is signed in. Saves this device's token and syncs topic subscriptions. */
    fun register(context: Context) {
        try {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { saveToken(it) }
            syncTopics(context)
        } catch (e: Exception) {
            Log.w("RetroPush", "FCM not available: ${e.message}")
        }
    }

    fun saveToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(mapOf("fcmTokens" to FieldValue.arrayUnion(token), "fcmUpdatedAt" to System.currentTimeMillis()), SetOptions.merge())
    }

    fun syncTopics(context: Context) {
        try {
            val fm = FirebaseMessaging.getInstance()
            RetroNotify.Channel.values().forEach { c ->
                val topic = c.topic ?: return@forEach
                if (RetroNotify.isEnabled(context, c)) fm.subscribeToTopic(topic) else fm.unsubscribeFromTopic(topic)
            }
        } catch (_: Exception) { }
    }
}

class RetroMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { RetroPush.saveToken(token) }

    override fun onMessageReceived(message: RemoteMessage) {
        val d = message.data
        val title = d["title"] ?: message.notification?.title ?: return
        val body = d["body"] ?: message.notification?.body ?: ""
        // Don't ping authors about their own article
        val me = FirebaseAuth.getInstance().currentUser?.uid
        if (!d["authorUid"].isNullOrBlank() && d["authorUid"] == me) return
        // Server sent the weekly push → the on-device worker shouldn't send a second one
        if (d["channel"] == RetroNotify.Channel.WEEKLY.id) {
            getSharedPreferences("retro_sync_state", Context.MODE_PRIVATE).edit()
                .putLong("weekly_last", System.currentTimeMillis()).apply()
        }
        RetroNotify.show(
            context = this,
            channel = RetroNotify.Channel.from(d["channel"]),
            key = d["key"] ?: message.messageId ?: title,
            title = title,
            body = body,
            nav = d["nav"],
            openWeekly = d["openWeekly"] == "true"
        )
    }
}

// ─── Background sync (every 6 hours) ──────────────────────────────────────────

object RetroSync {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RetroSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("retro_sync", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** "CHECK NOW" in notification settings. */
    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<RetroSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("retro_sync_now", ExistingWorkPolicy.REPLACE, request)
    }
}

private class SyncState(context: Context) {
    private val p = context.getSharedPreferences("retro_sync_state", Context.MODE_PRIVATE)
    fun set(k: String): Set<String> = p.getStringSet(k, emptySet())?.toSet() ?: emptySet()
    fun putSet(k: String, v: Set<String>) = p.edit().putStringSet(k, v).apply()
    fun long(k: String) = p.getLong(k, 0L)
    fun putLong(k: String, v: Long) = p.edit().putLong(k, v).apply()
    fun str(k: String) = p.getString(k, "") ?: ""
    fun putStr(k: String, v: String) = p.edit().putString(k, v).apply()
}

private fun dayKey(millis: Long) = SimpleDateFormat("yyyyMMdd", Locale.US).format(java.util.Date(millis))
private fun capped(s: Set<String>, max: Int) = if (s.size <= max) s else s.toList().takeLast(max).toSet()
private fun plural(n: Int, one: String, many: String) = if (n == 1) one else many

class RetroSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        val st = SyncState(c)
        runCatching { checkReleases(c, st) }.onFailure { Log.w(TAG, "releases: ${it.message}") }
        runCatching { checkNews(c, st) }.onFailure { Log.w(TAG, "news: ${it.message}") }
        runCatching { checkCommunity(c, st) }.onFailure { Log.w(TAG, "community: ${it.message}") }
        runCatching { checkSupport(c, st) }.onFailure { Log.w(TAG, "support: ${it.message}") }
        runCatching { checkWeekly(c, st) }.onFailure { Log.w(TAG, "weekly: ${it.message}") }
        return Result.success()
    }

    private suspend fun checkReleases(c: Context, st: SyncState) {
        IGDBRepository.clearReleaseCache()
        val all = (IGDBRepository.fetchUpcomingGames() + IGDBRepository.fetchUpcomingDLC()).distinctBy { it.id }
        if (all.isEmpty()) return
        val now = System.currentTimeMillis()
        val known = st.set("rel_known")
        val ids = all.map { it.id.toString() }.toSet()

        if (known.isNotEmpty()) {
            val added = all.filter { it.id.toString() !in known && it.releaseDate * 1000L > now }
                .sortedByDescending { it.hypes }
            if (added.isNotEmpty()) {
                val names = added.take(3).joinToString(", ") { it.name }
                val more = if (added.size > 3) " and ${added.size - 3} more" else ""
                RetroNotify.show(
                    c, RetroNotify.Channel.RELEASES, "rel_added_${dayKey(now)}",
                    "📅 ${added.size} new ${plural(added.size, "release", "releases")} on the calendar",
                    "$names$more",
                    nav = "EVENTS",
                    bigText = added.take(8).joinToString("\n") { g ->
                        "• ${g.name} — ${releaseTypeLabel(g.category, g.releaseDate)} · " +
                            SimpleDateFormat("MMM d", Locale.getDefault()).format(java.util.Date(g.releaseDate * 1000L))
                    }
                )
            }
        }

        // Out today
        val today = dayKey(now)
        val todays = all.filter { dayKey(it.releaseDate * 1000L) == today }
        val wish = ReleaseWishlist.load(c).toSet()
        val notifiedWish = st.set("wish_notified")
        val wishToday = todays.filter { it.id in wish && it.id.toString() !in notifiedWish }
        wishToday.forEach { g ->
            RetroNotify.show(
                c, RetroNotify.Channel.RELEASES, "wish_${g.id}",
                "⭐ ${g.name} is OUT TODAY!",
                "It's on your wishlist — ${g.platforms.take(3).joinToString(" · ")}",
                nav = "EVENTS"
            )
        }
        if (wishToday.isNotEmpty()) st.putSet("wish_notified", capped(notifiedWish + wishToday.map { it.id.toString() }, 500))

        if (st.str("out_today_day") != today && todays.isNotEmpty()) {
            val top = todays.sortedByDescending { it.hypes }.take(3)
            RetroNotify.show(
                c, RetroNotify.Channel.RELEASES, "out_today_$today",
                "🎮 Out today: ${top.first().name}",
                if (todays.size > 1) "${top.drop(1).joinToString(", ") { it.name }}${if (todays.size > 3) " + ${todays.size - 3} more" else ""}" else "Now available",
                nav = "EVENTS"
            )
            st.putStr("out_today_day", today)
        }

        st.putSet("rel_known", capped(known + ids, 5000))
    }

    private suspend fun checkNews(c: Context, st: SyncState) {
        val stories = LiveFeeds.fetchAll(force = true)
        if (stories.isEmpty()) return
        val known = st.set("news_known")
        val ids = stories.map { it.id }.toSet()
        if (known.isNotEmpty()) {
            val dayAgo = System.currentTimeMillis() - 24 * 3_600_000L
            val fresh = stories.filter { it.id !in known && it.publishedAt > dayAgo }
                .sortedWith(compareByDescending<ArticleItem> { it.category == "RETRO" }.thenByDescending { it.publishedAt })
            if (fresh.isNotEmpty()) {
                RetroNotify.show(
                    c, RetroNotify.Channel.NEWS, "news_batch",
                    "📰 ${fresh.size} new ${plural(fresh.size, "story", "stories")}",
                    fresh.first().title,
                    nav = "ARTICLES",
                    bigText = fresh.take(5).joinToString("\n") { "• ${it.title} (${it.sourceName})" }
                )
            }
        }
        st.putSet("news_known", capped(known + ids, 1500))
    }

    private suspend fun checkCommunity(c: Context, st: SyncState) {
        val me = FirebaseAuth.getInstance().currentUser?.uid
        val docs = FirebaseFirestore.getInstance().collection("articles")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(10).get().await()
        val items = docs.documents.mapNotNull { d ->
            val ts = d.getTimestamp("timestamp")?.toDate()?.time ?: return@mapNotNull null
            Triple(d.getString("title") ?: return@mapNotNull null, d.getString("authorUsername") ?: "someone", ts) to d.getString("authorUid")
        }
        val last = st.long("community_last")
        val newest = items.maxOfOrNull { it.first.third } ?: System.currentTimeMillis()
        if (last > 0L) {
            val fresh = items.filter { it.first.third > last && it.second != me }.map { it.first }
            if (fresh.isNotEmpty()) {
                val (title, author, _) = fresh.first()
                RetroNotify.show(
                    c, RetroNotify.Channel.COMMUNITY, "community_articles",
                    if (fresh.size == 1) "✍️ New community article" else "✍️ ${fresh.size} new community articles",
                    "“$title” by @$author",
                    nav = "ARTICLES",
                    bigText = fresh.take(5).joinToString("\n") { "• ${it.first} — @${it.second}" }
                )
            }
        }
        st.putLong("community_last", maxOf(last, newest))
    }

    private suspend fun checkSupport(c: Context, st: SyncState) {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val seeded = st.long("support_seeded") > 0L

        val tickets = db.collection("support_tickets").whereEqualTo("uid", me).get().await()
        val oldSigs = st.set("ticket_sigs")
        val sigs = mutableSetOf<String>()
        tickets.documents.forEach { d ->
            val status = d.getString("status") ?: "OPEN"
            val reply = d.getString("staffReply") ?: ""
            val sig = "${d.id}|$status|${reply.hashCode()}"
            sigs.add(sig)
            if (seeded && sig !in oldSigs && (reply.isNotBlank() || status != "OPEN")) {
                RetroNotify.show(
                    c, RetroNotify.Channel.SUPPORT, "ticket_${d.id}",
                    "🛟 Update on ticket #${d.getString("ticketNo") ?: ""}",
                    reply.ifBlank { "Status: ${status.replace('_', ' ')}" },
                    nav = "SUPPORT"
                )
            }
        }
        st.putSet("ticket_sigs", sigs)

        val mine = db.collection("petitions").whereEqualTo("creatorUid", me).get().await()
        val notified = st.set("petition_goal")
        val reached = mutableSetOf<String>()
        mine.documents.forEach { d ->
            val sig = (d.getLong("signatures") ?: 0L).toInt()
            val goal = (d.getLong("goal") ?: 50L).toInt()
            if (sig >= goal) {
                reached.add(d.id)
                if (seeded && d.id !in notified) {
                    RetroNotify.show(
                        c, RetroNotify.Channel.SUPPORT, "petition_${d.id}",
                        "📜 Your petition hit its goal!",
                        "“${d.getString("title") ?: ""}” reached $goal signatures — the team will review it.",
                        nav = "SUPPORT"
                    )
                }
            }
        }
        st.putSet("petition_goal", notified + reached)
        if (!seeded) st.putLong("support_seeded", System.currentTimeMillis())
    }

    private suspend fun checkWeekly(c: Context, st: SyncState) {
        val now = System.currentTimeMillis()
        val last = st.long("weekly_last")
        if (last == 0L) { st.putLong("weekly_last", now - 6 * 86_400_000L); return }  // first recap ~1 day after install
        if (now - last < 7 * 86_400_000L) return
        val d = WeeklyDigest.build(force = true)
        RetroNotify.show(
            c, RetroNotify.Channel.WEEKLY, "weekly_${dayKey(now)}",
            "🗓️ Your weekly RetroHub update is here",
            "${d.games.size} ${plural(d.games.size, "release", "releases")} · ${d.dlc.size} DLC & expansions · ${d.news.size} stories",
            nav = "HOME",
            openWeekly = true,
            bigText = buildString {
                if (d.games.isNotEmpty()) append("🎮 ${d.games.take(3).joinToString(", ") { it.name }}\n")
                if (d.dlc.isNotEmpty()) append("📦 ${d.dlc.take(2).joinToString(", ") { it.name }}\n")
                if (d.news.isNotEmpty()) append("📰 ${d.news.first().title}\n")
                if (d.community.isNotEmpty()) append("✍️ ${d.community.size} new community ${plural(d.community.size, "article", "articles")}")
            }.trim()
        )
        st.putLong("weekly_last", now)
    }

    companion object { private const val TAG = "RetroSync" }
}

// ─── Settings + permission UI ─────────────────────────────────────────────────

object NotificationSettingsBus { var open by mutableStateOf(false) }

/** Asks for the Android 13+ notification permission once, with a comic explainer first. */
@Composable
fun NotificationPermissionAsker() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT < 33) return
    val prefs = remember { context.getSharedPreferences("notif_prefs", Context.MODE_PRIVATE) }
    var show by remember { mutableStateOf(!RetroNotify.hasPermission(context) && !prefs.getBoolean("asked_permission", false)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { Chiptune.play(Chiptune.Sfx.POWER_UP); RetroSync.runNow(context) }
    }
    if (!show) return
    Dialog(onDismissRequest = { show = false; prefs.edit().putBoolean("asked_permission", true).apply() }) {
        Column(
            modifier = Modifier.comicPop().clip(RoundedCornerShape(16.dp)).background(Color.White)
                .border(3.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🔔", fontSize = 40.sp)
            Text("TURN ON ALERTS?", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 26.sp, letterSpacing = 1.sp, color = ScrapbookDark))
            Text(
                "Get pinged when new games, DLC & expansions hit the calendar, when your wishlist games come out, for fresh news, messages, and your WEEKLY UPDATE.",
                textAlign = TextAlign.Center,
                style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = ScrapbookDark)
            )
            RetroGlassButton(text = "YES, NOTIFY ME", modifier = Modifier.fillMaxWidth(), onClick = {
                prefs.edit().putBoolean("asked_permission", true).apply()
                show = false
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
            Text("MAYBE LATER", modifier = Modifier.clickable {
                prefs.edit().putBoolean("asked_permission", true).apply(); show = false
            }.padding(6.dp), style = TextStyle(fontFamily = BangersFontFamily, fontSize = 14.sp, color = ScrapbookTextMuted))
        }
    }
}

/** Per-channel toggles. Opened from the drawer bell. */
@Composable
fun NotificationSettingsHost() {
    if (!NotificationSettingsBus.open) return
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val permission = remember(refresh) { RetroNotify.hasPermission(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    Dialog(onDismissRequest = { NotificationSettingsBus.open = false }) {
        Box(modifier = Modifier.comicPop()) {
            Box(Modifier.matchParentSize().offset(6.dp, 6.dp).clip(RoundedCornerShape(16.dp)).background(CGreen))
            Column(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .border(3.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🔔 NOTIFICATIONS", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 24.sp, letterSpacing = 1.sp, color = ScrapbookDark))
                if (!permission) {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ComicGlassBg)
                            .border(1.5.dp, CAcRed, RoundedCornerShape(10.dp)).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Notifications are turned off for RetroHub on this phone.",
                            style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark))
                        RetroGlassButton(text = "TURN ON", modifier = Modifier.fillMaxWidth(), onClick = {
                            if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        })
                    }
                }
                RetroNotify.Channel.values().forEachIndexed { i, ch ->
                    var on by remember(ch) { mutableStateOf(RetroNotify.isEnabled(context, ch)) }
                    Row(
                        modifier = Modifier.fillMaxWidth().jumpIn(i),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(ch.emoji, fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ch.title.uppercase(), style = TextStyle(fontFamily = BangersFontFamily, fontSize = 15.sp, letterSpacing = 0.5.sp, color = ScrapbookDark))
                            Text(ch.desc, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, lineHeight = 14.sp, color = ScrapbookTextMuted))
                        }
                        Switch(
                            checked = on,
                            onCheckedChange = { v -> on = v; RetroNotify.setEnabled(context, ch, v); Chiptune.play(Chiptune.Sfx.BLIP) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White, checkedTrackColor = CGreen,
                                uncheckedThumbColor = Color.White, uncheckedTrackColor = ScrapbookDark.copy(alpha = 0.25f),
                                checkedBorderColor = ScrapbookDark, uncheckedBorderColor = ScrapbookDark
                            )
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RetroGlassButton(text = "CHECK NOW", modifier = Modifier.weight(1f), onClick = {
                        RetroSync.runNow(context); Chiptune.play(Chiptune.Sfx.COIN)
                    })
                    RetroGlassButton(text = "TEST", modifier = Modifier.weight(1f), onClick = {
                        RetroNotify.show(context, RetroNotify.Channel.WEEKLY, "test",
                            "🕹️ RetroHub alerts are ON", "Tap to open your weekly update.", nav = "HOME", openWeekly = true)
                    })
                }
                Text("DONE", modifier = Modifier.fillMaxWidth().clickable { NotificationSettingsBus.open = false }.padding(6.dp),
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontFamily = BangersFontFamily, fontSize = 15.sp, letterSpacing = 1.sp, color = CGreenDeep))
            }
        }
    }
}
