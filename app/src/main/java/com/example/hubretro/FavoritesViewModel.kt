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

// Data class to store a favorite item in Firestore
data class FavoriteItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String? = null,
    val webUrl: String = "",
    val category: String = "", // "ALBUM", "MAGAZINE", "ARTICLE"
    val creator: String? = null,
    val year: String? = null
)

// Convert ArchiveItem to FavoriteItem
fun ArchiveItem.toFavoriteItem() = FavoriteItem(
    id = this.id,
    title = this.title,
    description = this.description,
    thumbnailUrl = this.thumbnailUrl,
    webUrl = this.webUrl,
    category = this.category.name,
    creator = this.creator,
    year = this.year
)

// Convert Album to FavoriteItem
fun Album.toFavoriteItem() = FavoriteItem(
    id = this.id,
    title = this.title,
    description = this.artist,
    thumbnailUrl = null,
    webUrl = this.webPlaybackUrl ?: "",
    category = "ALBUM",
    creator = this.artist,
    year = this.year?.toString()
)

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    var activityViewModel: ActivityViewModel? = null

    private val _favorites = MutableStateFlow<List<FavoriteItem>>(emptyList())


    val favorites: StateFlow<List<FavoriteItem>> = _favorites.asStateFlow()

    // Set of favorite IDs for quick lookup
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchFavorites()
    }

    private fun getFavoritesRef() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("favorites")
    }

    fun fetchFavorites() {
        val ref = getFavoritesRef() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val docs = ref.get().await()
                val items = docs.documents.mapNotNull {
                    it.toObject(FavoriteItem::class.java)
                }
                _favorites.value = items
                _favoriteIds.value = items.map { it.id }.toSet()
            } catch (e: Exception) {
                // silently fail
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addFavorite(item: FavoriteItem) {
        val ref = getFavoritesRef() ?: return
        viewModelScope.launch {
            try {
                ref.document(item.id).set(item).await()
                _favorites.value = _favorites.value + item
                _favoriteIds.value = _favoriteIds.value + item.id
            } catch (e: Exception) {
                // silently fail
            }
        }
    }

    fun removeFavorite(itemId: String) {
        val ref = getFavoritesRef() ?: return
        viewModelScope.launch {
            try {
                ref.document(itemId).delete().await()
                _favorites.value = _favorites.value.filter { it.id != itemId }
                _favoriteIds.value = _favoriteIds.value - itemId
            } catch (e: Exception) {
                // silently fail
            }
        }
    }

    fun toggleFavorite(item: FavoriteItem) {
        if (_favoriteIds.value.contains(item.id)) {
            removeFavorite(item.id)
        } else {
            addFavorite(item)
        }
    }

    fun isFavorite(itemId: String): Boolean {
        return _favoriteIds.value.contains(itemId)
    }
}