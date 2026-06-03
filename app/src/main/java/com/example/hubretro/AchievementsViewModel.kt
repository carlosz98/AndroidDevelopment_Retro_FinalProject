package com.example.hubretro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object XPValues {
    const val JOIN = 25
    const val BOOKMARK = 10
    const val ARTICLE = 50
    const val FOLLOW = 15
}

data class RetroLevel(
    val level: Int,
    val title: String,
    val emoji: String,
    val minXP: Int,
    val maxXP: Int,
    val color: androidx.compose.ui.graphics.Color
)

val retroLevels = listOf(
    RetroLevel(1, "Insert Coin", "🕹️", 0, 99,
        androidx.compose.ui.graphics.Color(0xFF9E9E9E)),
    RetroLevel(2, "Player One", "👾", 100, 299,
        androidx.compose.ui.graphics.Color(0xFF4CAF50)),
    RetroLevel(3, "Adventurer", "⚔️", 300, 599,
        androidx.compose.ui.graphics.Color(0xFF2196F3)),
    RetroLevel(4, "High Scorer", "🏆", 600, 999,
        androidx.compose.ui.graphics.Color(0xFFFF9800)),
    RetroLevel(5, "Legend", "🌟", 1000, Int.MAX_VALUE,
        androidx.compose.ui.graphics.Color(0xFFE91E63))
)

fun getRetroLevel(xp: Int): RetroLevel =
    retroLevels.lastOrNull { xp >= it.minXP } ?: retroLevels.first()

fun getLevelProgress(xp: Int): Float {
    val level = getRetroLevel(xp)
    if (level.maxXP == Int.MAX_VALUE) return 1f
    val range = (level.maxXP - level.minXP + 1).toFloat()
    val progress = (xp - level.minXP).toFloat()
    return (progress / range).coerceIn(0f, 1f)
}

// ✅ Fixed — uses maxXP not nextLevelXp
fun getXpToNextLevel(xp: Int): Int {
    val level = getRetroLevel(xp)
    if (level.maxXP == Int.MAX_VALUE) return 0
    return (level.maxXP - xp + 1).coerceAtLeast(0)
}

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val color: androidx.compose.ui.graphics.Color,
    val isEarned: Boolean = false
)

fun buildBadges(
    hasJoined: Boolean,
    articleCount: Int,
    bookmarkCount: Int,
    followCount: Int,
    albumBookmarkCount: Int,
    magazineBookmarkCount: Int,
    profileComplete: Boolean
): List<Badge> = listOf(
    Badge(
        id = "newcomer",
        name = "Newcomer",
        description = "Joined RetroHub",
        emoji = "🎮",
        color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
        isEarned = hasJoined
    ),
    Badge(
        id = "scribe",
        name = "Scribe",
        description = "Wrote your first article",
        emoji = "📝",
        color = androidx.compose.ui.graphics.Color(0xFFE91E63),
        isEarned = articleCount >= 1
    ),
    Badge(
        id = "bibliophile",
        name = "Bibliophile",
        description = "Wrote 5 articles",
        emoji = "📚",
        color = androidx.compose.ui.graphics.Color(0xFF9C27B0),
        isEarned = articleCount >= 5
    ),
    Badge(
        id = "collector",
        name = "Collector",
        description = "First bookmark saved",
        emoji = "🔖",
        color = androidx.compose.ui.graphics.Color(0xFF2196F3),
        isEarned = bookmarkCount >= 1
    ),
    Badge(
        id = "archivist",
        name = "Archivist",
        description = "Saved 10 bookmarks",
        emoji = "🗂️",
        color = androidx.compose.ui.graphics.Color(0xFF00BCD4),
        isEarned = bookmarkCount >= 10
    ),
    Badge(
        id = "social",
        name = "Social",
        description = "Followed your first player",
        emoji = "👥",
        color = androidx.compose.ui.graphics.Color(0xFFFF9800),
        isEarned = followCount >= 1
    ),
    Badge(
        id = "audiophile",
        name = "Audiophile",
        description = "Bookmarked an album",
        emoji = "🎵",
        color = androidx.compose.ui.graphics.Color(0xFF8BC34A),
        isEarned = albumBookmarkCount >= 1
    ),
    Badge(
        id = "journalist",
        name = "Journalist",
        description = "Bookmarked a magazine",
        emoji = "📰",
        color = androidx.compose.ui.graphics.Color(0xFFFF5722),
        isEarned = magazineBookmarkCount >= 1
    ),
    Badge(
        id = "explorer",
        name = "Explorer",
        description = "Completed your profile",
        emoji = "🌐",
        color = androidx.compose.ui.graphics.Color(0xFF673AB7),
        isEarned = profileComplete
    )
)

data class AchievementsState(
    val xp: Int = 0,
    val badges: List<Badge> = emptyList(),
    val articleCount: Int = 0,
    val bookmarkCount: Int = 0,
    val followCount: Int = 0,
    val albumBookmarkCount: Int = 0,
    val magazineBookmarkCount: Int = 0
)

class AchievementsViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(AchievementsState())
    val state: StateFlow<AchievementsState> = _state.asStateFlow()

    init {
        fetchAchievements()
    }

    private fun getXpRef() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid)
            .collection("achievements").document("xp")
    }

    fun fetchAchievements() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val xpDoc = getXpRef()?.get()?.await()
                val xp = (xpDoc?.getLong("total") ?: 0L).toInt()
                val articleCount = (xpDoc?.getLong("articleCount") ?: 0L).toInt()
                val bookmarkCount = (xpDoc?.getLong("bookmarkCount") ?: 0L).toInt()
                val followCount = (xpDoc?.getLong("followCount") ?: 0L).toInt()
                val albumBookmarkCount = (xpDoc?.getLong("albumBookmarkCount") ?: 0L).toInt()
                val magazineBookmarkCount = (xpDoc?.getLong("magazineBookmarkCount") ?: 0L).toInt()

                val profileDoc = firestore.collection("users").document(uid).get().await()
                val profileComplete = listOf(
                    profileDoc.getString("bio"),
                    profileDoc.getString("location"),
                    profileDoc.getString("website")
                ).all { !it.isNullOrBlank() }

                val badges = buildBadges(
                    hasJoined = xp > 0,
                    articleCount = articleCount,
                    bookmarkCount = bookmarkCount,
                    followCount = followCount,
                    albumBookmarkCount = albumBookmarkCount,
                    magazineBookmarkCount = magazineBookmarkCount,
                    profileComplete = profileComplete
                )

                _state.value = AchievementsState(
                    xp = xp,
                    badges = badges,
                    articleCount = articleCount,
                    bookmarkCount = bookmarkCount,
                    followCount = followCount,
                    albumBookmarkCount = albumBookmarkCount,
                    magazineBookmarkCount = magazineBookmarkCount
                )
            } catch (e: Exception) { }
        }
    }

    fun awardXP(amount: Int, type: String) {
        val ref = getXpRef() ?: return
        viewModelScope.launch {
            try {
                val countField = when (type) {
                    "ARTICLE" -> "articleCount"
                    "BOOKMARK_ALBUM" -> "albumBookmarkCount"
                    "BOOKMARK_MAGAZINE" -> "magazineBookmarkCount"
                    "BOOKMARK" -> "bookmarkCount"
                    "FOLLOW" -> "followCount"
                    else -> null
                }
                val updates = hashMapOf<String, Any>(
                    "total" to com.google.firebase.firestore.FieldValue.increment(amount.toLong())
                )
                countField?.let {
                    updates[it] = com.google.firebase.firestore.FieldValue.increment(1L)
                }
                ref.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                fetchAchievements()
            } catch (e: Exception) { }
        }
    }

    fun refreshForUser() {
        fetchAchievements()
    }
}