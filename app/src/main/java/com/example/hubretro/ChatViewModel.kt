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
    val reactions: Map<String, List<String>> = emptyMap() // emoji -> list of uids
)

data class ChatRoom(
    val id: String = "",
    val type: String = "dm", // "dm" or "group"
    val name: String = "",
    val memberUids: List<String> = emptyList(),
    val memberUsernames: Map<String, String> = emptyMap(),
    val memberProfilePics: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSenderId: String = "",
    val unreadCounts: Map<String, Int> = emptyMap()
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

    // Listen to all chat rooms for current user
    fun listenToChatRooms() {
        val uid = currentUid
        if (uid.isBlank()) {
            _chatRooms.value = ChatUiState.Empty
            return
        }
        db.collection("chats")
            .whereArrayContains("memberUids", uid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _chatRooms.value = ChatUiState.Error(error.message ?: "Failed to load chats")
                    return@addSnapshotListener
                }
                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    ChatRoom(
                        id = doc.id,
                        type = data["type"] as? String ?: "dm",
                        name = data["name"] as? String ?: "",
                        memberUids = (data["memberUids"] as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList(),
                        memberUsernames = (data["memberUsernames"] as? Map<*, *>)
                            ?.entries?.associate { it.key.toString() to it.value.toString() }
                            ?: emptyMap(),
                        memberProfilePics = (data["memberProfilePics"] as? Map<*, *>)
                            ?.entries?.associate { it.key.toString() to it.value.toString() }
                            ?: emptyMap(),
                        lastMessage = data["lastMessage"] as? String ?: "",
                        lastMessageTimestamp = (data["lastMessageTimestamp"] as? Long) ?: 0L,
                        lastMessageSenderId = data["lastMessageSenderId"] as? String ?: "",
                        unreadCounts = (data["unreadCounts"] as? Map<*, *>)
                            ?.entries?.associate {
                                it.key.toString() to ((it.value as? Long)?.toInt() ?: 0)
                            } ?: emptyMap()
                    )
                } ?: emptyList()
                _chatRooms.value = if (rooms.isEmpty()) ChatUiState.Empty
                else ChatUiState.Success(rooms)
                _totalUnread.value = rooms.sumOf { it.unreadCounts[uid] ?: 0 }
            }
    }

    // Listen to messages in a chat room
    fun listenToMessages(chatId: String) {
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
                        reactions = (data["reactions"] as? Map<*, *>)
                            ?.entries?.associate { entry ->
                                entry.key.toString() to
                                        ((entry.value as? List<*>)?.filterIsInstance<String>()
                                            ?: emptyList())
                            } ?: emptyMap()
                    )
                } ?: emptyList()
                _messages.value = msgs
                // Mark as read
                markAsRead(chatId)
            }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    // Send text message
    fun sendMessage(chatId: String, text: String, profile: UserProfileData?) {
        if (text.isBlank()) return
        val uid = currentUid
        val msg = hashMapOf(
            "senderId" to uid,
            "senderUsername" to (profile?.username ?: ""),
            "senderProfilePicUrl" to (profile?.profilePictureUrl ?: ""),
            "text" to text.trim(),
            "imageUrl" to null,
            "timestamp" to System.currentTimeMillis(),
            "reactions" to emptyMap<String, List<String>>()
        )
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId)
                    .collection("messages").add(msg).await()
                updateLastMessage(chatId, text.trim(), uid)
            } catch (e: Exception) { }
        }
    }

    // Send image message
    fun sendImage(chatId: String, uri: Uri, profile: UserProfileData?) {
        val uid = currentUid
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val ref = storage.reference
                    .child("chat_images/$chatId/${UUID.randomUUID()}.jpg")
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                val msg = hashMapOf(
                    "senderId" to uid,
                    "senderUsername" to (profile?.username ?: ""),
                    "senderProfilePicUrl" to (profile?.profilePictureUrl ?: ""),
                    "text" to "",
                    "imageUrl" to downloadUrl,
                    "timestamp" to System.currentTimeMillis(),
                    "reactions" to emptyMap<String, List<String>>()
                )
                db.collection("chats").document(chatId)
                    .collection("messages").add(msg).await()
                updateLastMessage(chatId, "📷 Image", uid)
            } catch (e: Exception) { } finally {
                _isUploading.value = false
            }
        }
    }

    // Toggle reaction on a message
    fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        val uid = currentUid
        val msgRef = db.collection("chats").document(chatId)
            .collection("messages").document(messageId)
        viewModelScope.launch {
            try {
                val doc = msgRef.get().await()
                @Suppress("UNCHECKED_CAST")
                val reactions = (doc.data?.get("reactions") as? Map<*, *>)
                    ?.entries?.associate { entry ->
                        entry.key.toString() to
                                ((entry.value as? List<*>)?.filterIsInstance<String>()
                                    ?: emptyList())
                    }?.toMutableMap() ?: mutableMapOf()
                val currentList = reactions[emoji]?.toMutableList() ?: mutableListOf()
                if (currentList.contains(uid)) currentList.remove(uid)
                else currentList.add(uid)
                if (currentList.isEmpty()) reactions.remove(emoji)
                else reactions[emoji] = currentList
                msgRef.update("reactions", reactions).await()
            } catch (e: Exception) { }
        }
    }

    // Create or get existing DM
    fun getOrCreateDm(
        otherUser: UserProfileData,
        myProfile: UserProfileData?,
        onResult: (String) -> Unit
    ) {
        val uid = currentUid
        val otherId = otherUser.uid
        val chatId = listOf(uid, otherId).sorted().joinToString("_")
        viewModelScope.launch {
            try {
                val doc = db.collection("chats").document(chatId).get().await()
                if (!doc.exists()) {
                    val room = hashMapOf(
                        "type" to "dm",
                        "name" to "",
                        "memberUids" to listOf(uid, otherId),
                        "memberUsernames" to mapOf(
                            uid to (myProfile?.username ?: ""),
                            otherId to otherUser.username
                        ),
                        "memberProfilePics" to mapOf(
                            uid to (myProfile?.profilePictureUrl ?: ""),
                            otherId to (otherUser.profilePictureUrl ?: "")
                        ),
                        "lastMessage" to "",
                        "lastMessageTimestamp" to 0L,
                        "lastMessageSenderId" to "",
                        "unreadCounts" to mapOf(uid to 0, otherId to 0)
                    )
                    db.collection("chats").document(chatId).set(room).await()
                }
                onResult(chatId)
            } catch (e: Exception) { }
        }
    }

    // Create group chat
    fun createGroupChat(
        name: String,
        members: List<UserProfileData>,
        myProfile: UserProfileData?,
        onResult: (String) -> Unit
    ) {
        val uid = currentUid
        val allMembers = (members + listOfNotNull(myProfile?.let {
            UserProfileData(
                uid = uid,
                username = it.username,
                profilePictureUrl = it.profilePictureUrl ?: "",
                userHandle = it.userHandle,
                bio = it.bio,
                email = it.email,
                followersCount = it.followersCount,
                followingCount = it.followingCount,
                setupComplete = it.setupComplete,
                bannerUrl = it.bannerUrl,
                topGames = it.topGames,
                topSoundtracks = it.topSoundtracks
            )
        })).distinctBy { it.uid }
        viewModelScope.launch {
            try {
                val chatId = UUID.randomUUID().toString()
                val room = hashMapOf(
                    "type" to "group",
                    "name" to name,
                    "memberUids" to allMembers.map { it.uid },
                    "memberUsernames" to allMembers.associate { it.uid to it.username },
                    "memberProfilePics" to allMembers.associate {
                        it.uid to (it.profilePictureUrl ?: "")
                    },
                    "lastMessage" to "",
                    "lastMessageTimestamp" to 0L,
                    "lastMessageSenderId" to "",
                    "unreadCounts" to allMembers.associate { it.uid to 0 }
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
                val memberUids = (doc.data?.get("memberUids") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
                val unreadUpdate = memberUids
                    .filter { it != senderId }
                    .associate { "unreadCounts.$it" to FieldValue.increment(1) }
                val update = hashMapOf<String, Any>(
                    "lastMessage" to text,
                    "lastMessageTimestamp" to System.currentTimeMillis(),
                    "lastMessageSenderId" to senderId
                ) + unreadUpdate
                db.collection("chats").document(chatId).update(update).await()
            } catch (e: Exception) { }
        }
    }

    private fun markAsRead(chatId: String) {
        val uid = currentUid
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("chats").document(chatId)
                    .update("unreadCounts.$uid", 0).await()
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
}