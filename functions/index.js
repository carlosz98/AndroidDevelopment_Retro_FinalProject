/**
 * RetroHub — push notifications (Firebase Cloud Functions, 2nd gen / Cloud Run on GCP)
 *
 * Every push is a DATA message: the Android app (RetroMessagingService) builds the
 * notification itself so it lands in the right channel, respects the user's toggles,
 * and opens the right page ("nav").
 *
 * Deploy:   cd functions && npm install && cd .. && firebase deploy --only functions
 * Requires the Blaze (pay-as-you-go) plan — the free tier covers this app's volume.
 */
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { setGlobalOptions, logger } = require("firebase-functions/v2");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
setGlobalOptions({ region: "us-central1", maxInstances: 10 });
const db = getFirestore();

const WEEKLY_TIMEZONE = "America/New_York";

// ─── helpers ────────────────────────────────────────────────────────────────

/** FCM data payloads must be string → string. */
function toData(obj) {
  const out = {};
  for (const [k, v] of Object.entries(obj)) {
    if (v !== undefined && v !== null) out[k] = String(v);
  }
  return out;
}

/** Sends to every device of a user and prunes dead tokens. */
async function pushToUser(uid, payload) {
  const snap = await db.collection("users").doc(uid).get();
  if (!snap.exists) return;
  const tokens = (snap.get("fcmTokens") || []).filter(Boolean);
  if (!tokens.length) return;

  const res = await getMessaging().sendEachForMulticast({
    tokens,
    data: toData(payload),
    android: { priority: "high" },
  });

  const dead = [];
  res.responses.forEach((r, i) => {
    const code = r.error && r.error.code;
    if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
      dead.push(tokens[i]);
    }
  });
  if (dead.length) await snap.ref.update({ fcmTokens: FieldValue.arrayRemove(...dead) });
}

async function pushToTopic(topic, payload) {
  await getMessaging().send({ topic, data: toData(payload), android: { priority: "high" } });
}

const clip = (s, n) => (s && s.length > n ? s.slice(0, n - 1) + "…" : s || "");

// ─── 1. Chat messages → every other member of the chat ─────────────────────

exports.onChatMessage = onDocumentCreated("chats/{chatId}/messages/{msgId}", async (event) => {
  const m = event.data && event.data.data();
  if (!m || m.deleted) return;
  const chat = await db.collection("chats").doc(event.params.chatId).get();
  const members = chat.get("memberUids") || [];
  const sender = m.senderUsername ? `@${m.senderUsername}` : "Someone";
  const title = chat.get("type") === "group" ? `💬 ${chat.get("name") || "Group"} · ${sender}` : `💬 ${sender}`;
  const body = m.text ? clip(m.text, 140) : m.gifUrl ? "sent a GIF" : m.imageUrl ? "sent a photo" : "sent a message";

  await Promise.all(
    members
      .filter((uid) => uid !== m.senderId)
      .map((uid) => pushToUser(uid, { channel: "community", key: `chat_${event.params.chatId}`, title, body, nav: "MESSAGES" }))
  );
});

// ─── 2. Social (follow / comment / reaction / mention / repost) ─────────────
// The app already writes these to users/{uid}/notifications for the in-app bell.

exports.onSocialNotification = onDocumentCreated("users/{uid}/notifications/{nid}", async (event) => {
  const n = event.data && event.data.data();
  if (!n) return;
  const who = n.fromUsername ? `@${n.fromUsername}` : "Someone";
  const text = {
    follow: `${who} started following you`,
    reaction: `${who} reacted ${n.emoji || ""} to your post`,
    comment: `${who} commented on your post`,
    mention: `${who} mentioned you`,
    repost: `${who} reposted your post`,
  }[n.type] || `${who} interacted with you`;

  await pushToUser(event.params.uid, {
    channel: "community",
    key: `social_${event.params.nid}`,
    title: "🔔 RetroHub",
    body: n.targetPreview ? `${text}: “${clip(n.targetPreview, 80)}”` : text,
    nav: n.type === "follow" ? "PROFILE" : "HOME",
  });
});

// ─── 3. Support ticket replies / status changes ─────────────────────────────

exports.onTicketUpdated = onDocumentUpdated("support_tickets/{id}", async (event) => {
  const before = event.data.before.data() || {};
  const after = event.data.after.data();
  if (!after || !after.uid) return;
  const replyChanged = !!after.staffReply && after.staffReply !== (before.staffReply || "");
  const statusChanged = after.status !== before.status;
  if (!replyChanged && !statusChanged) return;

  await pushToUser(after.uid, {
    channel: "support",
    key: `ticket_${event.params.id}`,
    title: `🛟 Update on ticket #${after.ticketNo || ""}`,
    body: replyChanged ? clip(after.staffReply, 160) : `Status: ${String(after.status).replace("_", " ")}`,
    nav: "SUPPORT",
  });
});

// ─── 4. Petition reaches its goal → creator ─────────────────────────────────

exports.onPetitionUpdated = onDocumentUpdated("petitions/{id}", async (event) => {
  const b = event.data.before.data() || {};
  const a = event.data.after.data();
  if (!a || !a.creatorUid) return;
  const goal = a.goal || 50;
  if ((b.signatures || 0) < goal && (a.signatures || 0) >= goal) {
    await pushToUser(a.creatorUid, {
      channel: "support",
      key: `petition_${event.params.id}`,
      title: "📜 Your petition hit its goal!",
      body: `“${clip(a.title, 80)}” reached ${goal} signatures — the team will review it.`,
      nav: "SUPPORT",
    });
  }
});

// ─── 5. New community article → everyone subscribed to "community" ──────────

exports.onArticleCreated = onDocumentCreated("articles/{id}", async (event) => {
  const a = event.data && event.data.data();
  if (!a || !a.title) return;
  await pushToTopic("community", {
    channel: "community",
    key: "community_articles",
    title: "✍️ New community article",
    body: `“${clip(a.title, 90)}” by @${a.authorUsername || "someone"}`,
    nav: "ARTICLES",
    authorUid: a.authorUid || "",   // the app skips it on the author's own phone
  });
});

// ─── 6. Weekly update — every Monday morning ────────────────────────────────

exports.weeklyUpdate = onSchedule({ schedule: "every monday 10:00", timeZone: WEEKLY_TIMEZONE }, async () => {
  await pushToTopic("weekly", {
    channel: "weekly",
    key: "weekly_push",
    title: "🗓️ Your weekly RetroHub update is here",
    body: "New games, DLC & expansions, top stories and community picks for this week.",
    nav: "HOME",
    openWeekly: "true",
  });
  logger.info("Weekly update push sent");
});
