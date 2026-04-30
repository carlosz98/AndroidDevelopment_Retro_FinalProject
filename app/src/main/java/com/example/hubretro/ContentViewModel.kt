package com.example.hubretro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ContentState {
    object Idle : ContentState()
    object Loading : ContentState()
    data class Success(val items: List<ArchiveItem>) : ContentState()
    data class Error(val message: String) : ContentState()
}

class ContentViewModel(application: Application) : AndroidViewModel(application) {

    private val _albumsState = MutableStateFlow<ContentState>(ContentState.Idle)
    val albumsState: StateFlow<ContentState> = _albumsState.asStateFlow()

    private val _magazinesState = MutableStateFlow<ContentState>(ContentState.Idle)
    val magazinesState: StateFlow<ContentState> = _magazinesState.asStateFlow()

    private val _articlesState = MutableStateFlow<ContentState>(ContentState.Idle)
    val articlesState: StateFlow<ContentState> = _articlesState.asStateFlow()

    private val _searchState = MutableStateFlow<ContentState>(ContentState.Idle)
    val searchState: StateFlow<ContentState> = _searchState.asStateFlow()

    init {
        fetchAlbums()
        fetchMagazines()
        fetchArticles()
    }

    fun fetchAlbums(query: String = "") {
        viewModelScope.launch {
            _albumsState.value = ContentState.Loading
            try {
                val items = InternetArchiveRepository.fetchGameSoundtracks(query)
                _albumsState.value = if (items.isEmpty())
                    ContentState.Error("No albums found")
                else
                    ContentState.Success(items)
            } catch (e: Exception) {
                _albumsState.value = ContentState.Error(e.message ?: "Failed to load albums")
            }
        }
    }

    fun fetchMagazines(query: String = "") {
        viewModelScope.launch {
            _magazinesState.value = ContentState.Loading
            try {
                val items = InternetArchiveRepository.fetchRetroMagazines(query)
                _magazinesState.value = if (items.isEmpty())
                    ContentState.Error("No magazines found")
                else
                    ContentState.Success(items)
            } catch (e: Exception) {
                _magazinesState.value = ContentState.Error(e.message ?: "Failed to load magazines")
            }
        }
    }

    fun fetchArticles(query: String = "") {
        viewModelScope.launch {
            _articlesState.value = ContentState.Loading
            try {
                val items = InternetArchiveRepository.fetchRetroArticles(query)
                _articlesState.value = if (items.isEmpty())
                    ContentState.Error("No articles found")
                else
                    ContentState.Success(items)
            } catch (e: Exception) {
                _articlesState.value = ContentState.Error(e.message ?: "Failed to load articles")
            }
        }
    }

    fun searchAll(query: String) {
        if (query.isBlank()) {
            _searchState.value = ContentState.Idle
            return
        }
        viewModelScope.launch {
            _searchState.value = ContentState.Loading
            try {
                val items = InternetArchiveRepository.searchAll(query)
                _searchState.value = if (items.isEmpty())
                    ContentState.Error("No results found for \"$query\"")
                else
                    ContentState.Success(items)
            } catch (e: Exception) {
                _searchState.value = ContentState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun resetSearch() {
        _searchState.value = ContentState.Idle
    }
}