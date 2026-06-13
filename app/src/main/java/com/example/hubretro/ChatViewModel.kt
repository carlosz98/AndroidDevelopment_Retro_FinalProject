package com.example.hubretro

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderProfilePicUrl: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0L,
    val reactions: Map<String, List<String>> = emptyMap(),
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val readBy: List<String> = emptyList(),
    val deleted: Boolean = false
)

data class ChatRoom(
    val id: String = "",
    val type: String = "dm",
    val name: String = "",
    val memberUids: List<String> = emptyList(),
    val memberUsernames: Map<String, String> = emptyMap(),
    val memberProfilePics: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSenderId: String = "",
    val unreadCounts: Map<String, Int> = emptyMap(),
    val pinnedMessageId: String? = null,
    val pinnedMessageText: String? = null,
    val backgroundKey: String = "default",
    val typingUids: List<String> = emptyList()
)

sealed class ChatUiState {
    object Loading : ChatUiState()
    object Empty : ChatUiState()
    data class Success(val rooms: List<ChatRoom>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val currentUid get() = auth.currentUser?.uid ?: ""

    private val _chatRooms = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val chatRooms: StateFlow<ChatUiState> = _chatRooms

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _totalUnread = MutableStateFlow(0)
    val totalUnread: StateFlow<Int> = _totalUnread

    private val _typingUids = MutableStateFlow<List<String>>(emptyList())
    val typingUids: StateFlow<List<String>> = _typingUids

    private val _onlineUsers = MutableStateFlow<Set<String>>(emptySet())
    val onlineUsers: StateFlow<Set<String>> = _onlineUsers

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private var typingJob: kotlinx.coroutines.Job? = null
    private var currentChatId: String = ""

    // ── Presence ──────────────────────────────────────────────────────────────

    fun setOnline() {
        val uid = currentUid
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("presence").document(uid)
                    .set(mapOf("online" to true, "lastSeen" to System.currentTimeMillis())).await()
            } catch (e: Exception) { }
        }
    }

    fun setOffline() {
        val uid = currentUid
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("presence").document(uid)
                    .set(mapOf("online" to false, "lastSeen" to System.currentTimeMillis())).await()
            } catch (e: Exception) { }
        }
    }

    fun listenToPresence(uids: List<String>) {
        if (uids.isEmpty()) return
        uids.forEach { uid ->
            db.collection("presence").document(uid)
                .addSnapshotListener { snap, _ ->
                    val isOnline = snap?.getBoolean("online") ?: false
                    val current = _onlineUsers.value.toMutableSet()
                    if (isOnline) current.add(uid) else current.remove(uid)
                    _onlineUsers.value = current
                }
        }
    }

    // ── Typing ────────────────────────────────────────────────────────────────

    fun onTyping(chatId: String) {
        val uid = currentUid
        if (uid.isBlank()) return
        typingJob?.cancel()
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId)
                    .update("typingUids", FieldValue.arrayUnion(uid)).await()
            } catch (e: Exception) { }
        }
        typingJob = viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            stopTyping(chatId)
        }
    }

    fun stopTyping(chatId: String) {
        val uid = currentUid
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId)
                    .update("typingUids", FieldValue.arrayRemove(uid)).await()
            } catch (e: Exception) { }
        }
    }

    // ── Chat rooms ────────────────────────────────────────────────────────────

    fun listenToChatRooms() {
        val uid = currentUid
        if (uid.isBlank()) { _chatRooms.value = ChatUiState.Empty; return }
        db.collection("chats")
            .whereArrayContains("memberUids", uid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { _chatRooms.value = ChatUiState.Error(error.message ?: "Failed"); return@addSnapshotListener }
                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    ChatRoom(
                        id = doc.id,
                        type = data["type"] as? String ?: "dm",
                        name = data["name"] as? String ?: "",
                        memberUids = (data["memberUids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        memberUsernames = (data["memberUsernames"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap(),
                        memberProfilePics = (data["memberProfilePics"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap(),
                        lastMessage = data["lastMessage"] as? String ?: "",
                        lastMessageTimestamp = (data["lastMessageTimestamp"] as? Long) ?: 0L,
                        lastMessageSenderId = data["lastMessageSenderId"] as? String ?: "",
                        unreadCounts = (data["unreadCounts"] as? Map<*, *>)?.entries?.associate { it.key.toString() to ((it.value as? Long)?.toInt() ?: 0) } ?: emptyMap(),
                        pinnedMessageId = data["pinnedMessageId"] as? String,
                        pinnedMessageText = data["pinnedMessageText"] as? String,
                        backgroundKey = data["backgroundKey"] as? String ?: "default",
                        typingUids = (data["typingUids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                } ?: emptyList()
                _chatRooms.value = if (rooms.isEmpty()) ChatUiState.Empty else ChatUiState.Success(rooms)
                _totalUnread.value = rooms.sumOf { it.unreadCounts[uid] ?: 0 }
                // listen to presence for all members
                val allUids = rooms.flatMap { it.memberUids }.distinct()
                listenToPresence(allUids)
            }
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    fun listenToMessages(chatId: String) {
        currentChatId = chatId
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val msgs = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    ChatMessage(
                        id = doc.id,
                        senderId = data["senderId"] as? String ?: "",
                        senderUsername = data["senderUsername"] as? String ?: "",
                        senderProfilePicUrl = data["senderProfilePicUrl"] as? String ?: "",
                        text = data["text"] as? String ?: "",
                        imageUrl = data["imageUrl"] as? String,
                        timestamp = (data["timestamp"] as? Long) ?: 0L,
                        reactions = (data["reactions"] as? Map<*, *>)?.entries?.associate { entry ->
                            entry.key.toString() to ((entry.value as? List<*>)?.filterIsInstance<String>() ?: emptyList())
                        } ?: emptyMap(),
                        replyToId = data["replyToId"] as? String,
                        replyToText = data["replyToText"] as? String,
                        replyToSender = data["replyToSender"] as? String,
                        readBy = (data["readBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        deleted = data["deleted"] as? Boolean ?: false
                    )
                } ?: emptyList()
                _messages.value = msgs
                markAsRead(chatId)
                // mark last message as read
                msgs.lastOrNull()?.let { markMessageRead(chatId, it.id) }
            }

        // listen to typing
        db.collection("chats").document(chatId)
            .addSnapshotListener { snap, _ ->
                val typing = (snap?.data?.get("typingUids") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.filter { it != currentUid } ?: emptyList()
                _typingUids.value = typing
            }
    }

    fun clearMessages() {
        _messages.value = emptyList()
        currentChatId = ""
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    fun sendMessage(
        chatId: String,
        text: String,
        profile: UserProfileData?,
        replyTo: ChatMessage? = null
    ) {
        if (text.isBlank()) return
        val uid = currentUid
        val msg = hashMapOf(
            "senderId" to uid,
            "senderUsername" to (profile?.username ?: ""),
            "senderProfilePicUrl" to (profile?.profilePictureUrl ?: ""),
            "text" to text.trim(),
            "imageUrl" to null,
            "timestamp" to System.currentTimeMillis(),
            "reactions" to emptyMap<String, List<String>>(),
            "replyToId" to replyTo?.id,
            "replyToText" to replyTo?.text?.take(80),
            "replyToSender" to replyTo?.senderUsername,
            "readBy" to listOf(uid),
            "deleted" to false
        )
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId).collection("messages").add(msg).await()
                updateLastMessage(chatId, text.trim(), uid)
                stopTyping(chatId)
            } catch (e: Exception) { }
        }
    }

    fun sendImage(chatId: String, uri: Uri, profile: UserProfileData?) {
        val uid = currentUid
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val ref = storage.reference.child("chat_images/$chatId/${UUID.randomUUID()}.jpg")
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                val msg = hashMapOf(
                    "senderId" to uid,
                    "senderUsername" to (profile?.username ?: ""),
                    "senderProfilePicUrl" to (profile?.profilePictureUrl ?: ""),
                    "text" to "",
                    "imageUrl" to downloadUrl,
                    "timestamp" to System.currentTimeMillis(),
                    "reactions" to emptyMap<String, List<String>>(),
                    "replyToId" to null,
                    "replyToText" to null,
                    "replyToSender" to null,
                    "readBy" to listOf(uid),
                    "deleted" to false
                )
                db.collection("chats").document(chatId).collection("messages").add(msg).await()
                updateLastMessage(chatId, "📷 Image", uid)
            } catch (e: Exception) { } finally {
                _isUploading.value = false
            }
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId)
                    .collection("messages").document(messageId)
                    .update("deleted", true, "text", "This message was deleted").await()
            } catch (e: Exception) { }
        }
    }

    // ── Reactions ─────────────────────────────────────────────────────────────

    fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        val uid = currentUid
        val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        viewModelScope.launch {
            try {
                val doc = msgRef.get().await()
                @Suppress("UNCHECKED_CAST")
                val reactions = (doc.data?.get("reactions") as? Map<*, *>)?.entries?.associate { entry ->
                    entry.key.toString() to ((entry.value as? List<*>)?.filterIsInstance<String>() ?: emptyList())
                }?.toMutableMap() ?: mutableMapOf()
                val currentList = reactions[emoji]?.toMutableList() ?: mutableListOf()
                if (currentList.contains(uid)) currentList.remove(uid) else currentList.add(uid)
                if (currentList.isEmpty()) reactions.remove(emoji) else reactions[emoji] = currentList
                msgRef.update("reactions", reactions).await()
            } catch (e: Exception) { }
        }
    }

    // ── Pin message ───────────────────────────────────────────────────────────

    fun pinMessage(chatId: String, message: ChatMessage) {
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId).update(
                    mapOf("pinnedMessageId" to message.id, "pinnedMessageText" to message.text.take(60))
                ).await()
            } catch (e: Exception) { }
        }
    }

    fun unpinMessage(chatId: String) {
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId).update(
                    mapOf("pinnedMessageId" to null, "pinnedMessageText" to null)
                ).await()
            } catch (e: Exception) { }
        }
    }

    // ── Background ────────────────────────────────────────────────────────────

    fun setChatBackground(chatId: String, backgroundKey: String) {
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId)
                    .update("backgroundKey", backgroundKey).await()
            } catch (e: Exception) { }
        }
    }

    // ── Read receipts ─────────────────────────────────────────────────────────

    private fun markMessageRead(chatId: String, messageId: String) {
        val uid = currentUid
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId)
                    .collection("messages").document(messageId)
                    .update("readBy", FieldValue.arrayUnion(uid)).await()
            } catch (e: Exception) { }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    // ── DM / Group ────────────────────────────────────────────────────────────

    fun getOrCreateDm(otherUser: UserProfileData, myProfile: UserProfileData?, onResult: (String) -> Unit) {
        val uid = currentUid
        val otherId = otherUser.uid
        val chatId = listOf(uid, otherId).sorted().joinToString("_")
        viewModelScope.launch {
            try {
                val doc = db.collection("chats").document(chatId).get().await()
                if (!doc.exists()) {
                    val room = hashMapOf(
                        "type" to "dm", "name" to "",
                        "memberUids" to listOf(uid, otherId),
                        "memberUsernames" to mapOf(uid to (myProfile?.username ?: ""), otherId to otherUser.username),
                        "memberProfilePics" to mapOf(uid to (myProfile?.profilePictureUrl ?: ""), otherId to (otherUser.profilePictureUrl ?: "")),
                        "lastMessage" to "", "lastMessageTimestamp" to 0L,
                        "lastMessageSenderId" to "",
                        "unreadCounts" to mapOf(uid to 0, otherId to 0),
                        "backgroundKey" to "default",
                        "typingUids" to emptyList<String>()
                    )
                    db.collection("chats").document(chatId).set(room).await()
                }
                onResult(chatId)
            } catch (e: Exception) { }
        }
    }

    fun createGroupChat(name: String, members: List<UserProfileData>, myProfile: UserProfileData?, onResult: (String) -> Unit) {
        val uid = currentUid
        val allMembers = (members + listOfNotNull(myProfile?.let {
            UserProfileData(uid = uid, username = it.username, profilePictureUrl = it.profilePictureUrl ?: "", userHandle = it.userHandle, bio = it.bio, email = it.email, followersCount = it.followersCount, followingCount = it.followingCount, setupComplete = it.setupComplete, bannerUrl = it.bannerUrl, topGames = it.topGames, topSoundtracks = it.topSoundtracks)
        })).distinctBy { it.uid }
        viewModelScope.launch {
            try {
                val chatId = UUID.randomUUID().toString()
                val room = hashMapOf(
                    "type" to "group", "name" to name,
                    "memberUids" to allMembers.map { it.uid },
                    "memberUsernames" to allMembers.associate { it.uid to it.username },
                    "memberProfilePics" to allMembers.associate { it.uid to (it.profilePictureUrl ?: "") },
                    "lastMessage" to "", "lastMessageTimestamp" to 0L,
                    "lastMessageSenderId" to "",
                    "unreadCounts" to allMembers.associate { it.uid to 0 },
                    "backgroundKey" to "default",
                    "typingUids" to emptyList<String>()
                )
                db.collection("chats").document(chatId).set(room).await()
                onResult(chatId)
            } catch (e: Exception) { }
        }
    }

    private fun updateLastMessage(chatId: String, text: String, senderId: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("chats").document(chatId).get().await()
                val memberUids = (doc.data?.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val unreadUpdate = memberUids.filter { it != senderId }.associate { "unreadCounts.$it" to FieldValue.increment(1) }
                val update = hashMapOf<String, Any>("lastMessage" to text, "lastMessageTimestamp" to System.currentTimeMillis(), "lastMessageSenderId" to senderId) + unreadUpdate
                db.collection("chats").document(chatId).update(update).await()
            } catch (e: Exception) { }
        }
    }

    private fun markAsRead(chatId: String) {
        val uid = currentUid
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId).update("unreadCounts.$uid", 0).await()
            } catch (e: Exception) { }
        }
    }

    fun getChatDisplayName(room: ChatRoom): String {
        if (room.type == "group") return room.name
        val otherId = room.memberUids.firstOrNull { it != currentUid } ?: return "Chat"
        return room.memberUsernames[otherId] ?: "Chat"
    }

    fun getChatProfilePic(room: ChatRoom): String {
        if (room.type == "group") return ""
        val otherId = room.memberUids.firstOrNull { it != currentUid } ?: return ""
        return room.memberProfilePics[otherId] ?: ""
    }

    fun getOtherUid(room: ChatRoom): String {
        return room.memberUids.firstOrNull { it != currentUid } ?: ""
    }
}