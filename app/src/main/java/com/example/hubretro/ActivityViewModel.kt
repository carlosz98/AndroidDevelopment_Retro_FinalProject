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

data class ActivityEntry(
    val id: String = "",
    val type: String = "",
    val description: String = "",
    val itemTitle: String = "",
    val itemCategory: String = "",
    val timestamp: Long = 0L
)

fun ActivityEntry.timeAgoString(): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60000
    val hours = diff / 3600000
    val days = diff / 86400000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _activities = MutableStateFlow<List<ActivityEntry>>(emptyList())
    val activities: StateFlow<List<ActivityEntry>> = _activities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchActivities()
    }

    private fun getActivityRef() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("activity")
    }

    fun fetchActivities() {
        val ref = getActivityRef() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Try with ordering first
                val docs = try {
                    ref
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(20)
                        .get()
                        .await()
                } catch (e: Exception) {
                    // Fallback without ordering if index not ready
                    ref.limit(20).get().await()
                }

                _activities.value = docs.documents.mapNotNull { doc ->
                    ActivityEntry(
                        id = doc.id,
                        type = doc.getString("type") ?: "",
                        description = doc.getString("description") ?: "",
                        itemTitle = doc.getString("itemTitle") ?: "",
                        itemCategory = doc.getString("itemCategory") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }.sortedByDescending { it.timestamp } // sort in memory as fallback
            } catch (e: Exception) {
                // silently fail
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logBookmarkActivity(item: FavoriteItem, isAdding: Boolean) {
        val ref = getActivityRef() ?: return
        if (!isAdding) return
        viewModelScope.launch {
            try {
                val categoryLabel = when (item.category) {
                    "ALBUM" -> "album"
                    "MAGAZINE" -> "magazine"
                    "ARTICLE" -> "article"
                    else -> "item"
                }
                val entry = hashMapOf(
                    "type" to "BOOKMARK",
                    "description" to "Bookmarked a $categoryLabel: \"${item.title}\"",
                    "itemTitle" to item.title,
                    "itemCategory" to item.category,
                    "timestamp" to System.currentTimeMillis()
                )
                ref.add(entry).await()
                fetchActivities() // refresh after logging
            } catch (e: Exception) { }
        }
    }

    fun logArticleActivity(article: ArticleItem) {
        val ref = getActivityRef() ?: return
        viewModelScope.launch {
            try {
                val entry = hashMapOf(
                    "type" to "ARTICLE",
                    "description" to "Wrote an article: \"${article.title}\"",
                    "itemTitle" to article.title,
                    "itemCategory" to "ARTICLE",
                    "timestamp" to System.currentTimeMillis()
                )
                ref.add(entry).await()
                fetchActivities() // refresh after logging
            } catch (e: Exception) { }
        }
    }

    // Call this when user logs in to refresh
    fun refreshForUser() {
        fetchActivities()
    }
}