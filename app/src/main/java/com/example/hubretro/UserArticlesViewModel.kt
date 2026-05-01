package com.example.hubretro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserArticle(
    val id: String = "",
    val title: String = "",
    val snippet: String = "",
    val fullContent: String = "",
    val youtubeVideoId: String = "",
    val headerImageUrl: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val timestamp: Long = 0L
) {
    fun formattedDate(): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun toArticleItem(): ArticleItem = ArticleItem(
        id = "user_$id",
        title = title,
        snippet = snippet,
        fullContent = fullContent,
        date = formattedDate(),
        author = authorUsername,
        imageResId = null,
        imageUrl = headerImageUrl.ifBlank { null },
        youtubeVideoId = youtubeVideoId.ifBlank { null }
    )
}

class UserArticlesViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _userArticles = MutableStateFlow<List<UserArticle>>(emptyList())
    val userArticles: StateFlow<List<UserArticle>> = _userArticles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _publishState = MutableStateFlow<PublishState>(PublishState.Idle)
    val publishState: StateFlow<PublishState> = _publishState.asStateFlow()

    sealed class PublishState {
        object Idle : PublishState()
        object Loading : PublishState()
        object Success : PublishState()
        data class Error(val message: String) : PublishState()
    }

    init {
        fetchAllUserArticles()
    }

    fun fetchAllUserArticles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val docs = firestore.collection("articles")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
                _userArticles.value = docs.documents.mapNotNull { doc ->
                    UserArticle(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        snippet = doc.getString("snippet") ?: "",
                        fullContent = doc.getString("fullContent") ?: "",
                        youtubeVideoId = doc.getString("youtubeVideoId") ?: "",
                        headerImageUrl = doc.getString("headerImageUrl") ?: "",
                        authorUid = doc.getString("authorUid") ?: "",
                        authorUsername = doc.getString("authorUsername") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }.filter { it.title.isNotBlank() }
            } catch (e: Exception) {
                // silently fail
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun publishArticle(
        title: String,
        snippet: String,
        fullContent: String,
        youtubeVideoId: String,
        headerImageUrl: String = "",
        username: String,
        activityViewModel: ActivityViewModel? = null
    ) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _publishState.value = PublishState.Loading
            try {
                val article = hashMapOf(
                    "title" to title,
                    "snippet" to snippet,
                    "fullContent" to fullContent,
                    "youtubeVideoId" to youtubeVideoId,
                    "headerImageUrl" to headerImageUrl,
                    "authorUid" to uid,
                    "authorUsername" to username,
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("articles").add(article).await()
                _publishState.value = PublishState.Success

                // Log activity
                activityViewModel?.logArticleActivity(
                    ArticleItem(
                        id = "",
                        title = title,
                        snippet = snippet,
                        fullContent = fullContent,
                        author = username
                    )
                )

                // Refresh articles
                fetchAllUserArticles()
            } catch (e: Exception) {
                _publishState.value = PublishState.Error(e.message ?: "Failed to publish")
            }
        }
    }

    fun resetPublishState() {
        _publishState.value = PublishState.Idle
    }
}