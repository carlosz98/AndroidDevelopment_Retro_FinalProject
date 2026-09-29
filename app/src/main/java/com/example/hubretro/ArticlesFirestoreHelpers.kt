package com.example.hubretro

import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// ─── Firestore Repository ──────────────────────────────────────────────────────

object ArticleFirestoreRepo {

    private val db = FirebaseFirestore.getInstance()

    fun articleDocRef(articleId: String) = db.collection("article_engagement").document(articleId)
    private fun likesCollection(articleId: String) = articleDocRef(articleId).collection("likes")
    private fun savesCollection(articleId: String) = articleDocRef(articleId).collection("saves")
    private fun reactionsCollection(articleId: String) = articleDocRef(articleId).collection("reactions")
    private fun commentsQuery(articleId: String) =
        db.collection("article_comments").whereEqualTo("articleId", articleId)
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(100)

    suspend fun fetchEngagement(articleId: String, uid: String?): ArticleEngagement {
        return try {
            val doc = articleDocRef(articleId).get().await()
            val likeCount = (doc.getLong("likeCount") ?: 0L).toInt()
            val saveCount = (doc.getLong("saveCount") ?: 0L).toInt()
            val commentCount = (doc.getLong("commentCount") ?: 0L).toInt()
            @Suppress("UNCHECKED_CAST")
            val reactionsMap = (doc.get("reactions") as? Map<String, Long>)?.mapValues { it.value.toInt() } ?: emptyMap()

            var isLiked = false
            var isSaved = false
            var myReaction: String? = null
            if (uid != null) {
                isLiked = likesCollection(articleId).document(uid).get().await().exists()
                isSaved = savesCollection(articleId).document(uid).get().await().exists()
                val reactDoc = reactionsCollection(articleId).document(uid).get().await()
                myReaction = if (reactDoc.exists()) reactDoc.getString("emoji") else null
            }

            ArticleEngagement(
                likeCount = likeCount, saveCount = saveCount, commentCount = commentCount,
                reactions = reactionsMap, isLiked = isLiked, isSaved = isSaved, myReaction = myReaction
            )
        } catch (e: Exception) { ArticleEngagement() }
    }

    suspend fun toggleLike(articleId: String, uid: String, currentlyLiked: Boolean): Boolean {
        return try {
            val ref = likesCollection(articleId).document(uid)
            if (currentlyLiked) {
                ref.delete().await()
                articleDocRef(articleId).set(mapOf("likeCount" to FieldValue.increment(-1)), com.google.firebase.firestore.SetOptions.merge()).await()
            } else {
                ref.set(mapOf("timestamp" to System.currentTimeMillis())).await()
                articleDocRef(articleId).set(mapOf("likeCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge()).await()
            }
            !currentlyLiked
        } catch (e: Exception) { currentlyLiked }
    }

    suspend fun toggleSave(articleId: String, uid: String, currentlySaved: Boolean): Boolean {
        return try {
            val ref = savesCollection(articleId).document(uid)
            if (currentlySaved) {
                ref.delete().await()
                articleDocRef(articleId).set(mapOf("saveCount" to FieldValue.increment(-1)), com.google.firebase.firestore.SetOptions.merge()).await()
            } else {
                ref.set(mapOf("timestamp" to System.currentTimeMillis())).await()
                articleDocRef(articleId).set(mapOf("saveCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge()).await()
            }
            !currentlySaved
        } catch (e: Exception) { currentlySaved }
    }

    suspend fun setReaction(articleId: String, uid: String, emoji: String, previousReaction: String?): Map<String, Int> {
        return try {
            val ref = reactionsCollection(articleId).document(uid)
            val updates = mutableMapOf<String, Any>()
            if (previousReaction == emoji) {
                ref.delete().await()
                updates["reactions.$emoji"] = FieldValue.increment(-1)
            } else {
                if (previousReaction != null) updates["reactions.$previousReaction"] = FieldValue.increment(-1)
                ref.set(mapOf("emoji" to emoji, "timestamp" to System.currentTimeMillis())).await()
                updates["reactions.$emoji"] = FieldValue.increment(1)
            }
            articleDocRef(articleId).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            val doc = articleDocRef(articleId).get().await()
            @Suppress("UNCHECKED_CAST")
            (doc.get("reactions") as? Map<String, Long>)?.mapValues { it.value.toInt() } ?: emptyMap()
        } catch (e: Exception) { emptyMap() }
    }

    suspend fun fetchComments(articleId: String): List<ArticleComment> {
        return try {
            val docs = commentsQuery(articleId).get().await()
            docs.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                ArticleComment(
                    id = doc.id, articleId = data["articleId"] as? String ?: "",
                    authorUid = data["userId"] as? String ?: "", authorUsername = data["username"] as? String ?: "",
                    authorPicUrl = data["profilePicUrl"] as? String ?: "", text = data["text"] as? String ?: "",
                    timestamp = data["timestamp"] as? Long ?: 0L
                )
            }.filter { it.text.isNotBlank() }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun postComment(articleId: String, uid: String, username: String, profilePicUrl: String, text: String): ArticleComment? {
        val text = Moderation.gate(text, "article_comment") ?: return null
        return try {
            val data = hashMapOf(
                "articleId" to articleId, "userId" to uid, "username" to username,
                "profilePicUrl" to profilePicUrl, "text" to text, "timestamp" to System.currentTimeMillis()
            )
            val ref = db.collection("article_comments").add(data).await()
            articleDocRef(articleId).set(mapOf("commentCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge()).await()
            ArticleComment(id = ref.id, articleId = articleId, authorUid = uid, authorUsername = username, authorPicUrl = profilePicUrl, text = text, timestamp = System.currentTimeMillis())
        } catch (e: Exception) { null }
    }

    suspend fun deleteComment(articleId: String, commentId: String) {
        try {
            db.collection("article_comments").document(commentId).delete().await()
            articleDocRef(articleId).set(mapOf("commentCount" to FieldValue.increment(-1)), com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) { }
    }

    suspend fun fetchSavedArticleIds(uid: String): Set<String> {
        return try {
            val docs = db.collectionGroup("saves").get().await()
            docs.documents.filter { it.id == uid }.mapNotNull { it.reference.parent.parent?.id }.toSet()
        } catch (e: Exception) { emptySet() }
    }

    suspend fun incrementViewCount(articleId: String) {
        try {
            articleDocRef(articleId).set(mapOf("viewCount" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) { }
    }

    suspend fun fetchViewCount(articleId: String): Int {
        return try {
            (articleDocRef(articleId).get().await().getLong("viewCount") ?: 0L).toInt()
        } catch (e: Exception) { 0 }
    }
}

// ─── Remember helper hook ──────────────────────────────────────────────────────

@Composable
fun rememberArticleEngagement(articleId: String): MutableState<ArticleEngagement> {
    val state = remember(articleId) { mutableStateOf(ArticleEngagement()) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(articleId) {
        state.value = ArticleFirestoreRepo.fetchEngagement(articleId, uid)
    }
    return state
}