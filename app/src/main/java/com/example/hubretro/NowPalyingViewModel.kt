package com.example.hubretro

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The album "Now Playing" state shown in the mini player and the album sheet.
 * Audio itself runs in RetroPlaybackService (via RetroPlayer), so it keeps playing in the
 * background and can be controlled from the lock screen; this ViewModel mirrors its state.
 */
class NowPlayingViewModel : ViewModel() {

    private val _nowPlaying = MutableStateFlow<NowPlayingState?>(null)
    val nowPlaying: StateFlow<NowPlayingState?> = _nowPlaying.asStateFlow()

    private val _tracks = MutableStateFlow<List<AlbumTrack>>(emptyList())
    val tracks: StateFlow<List<AlbumTrack>> = _tracks.asStateFlow()

    private val _selectedTrack = MutableStateFlow<AlbumTrack?>(null)
    val selectedTrack: StateFlow<AlbumTrack?> = _selectedTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Kept for older callers; playback no longer goes through a WebView.
    var webViewRef: WebView? = null

    init {
        // Follow the real player (lock-screen / headphone buttons change it too)
        viewModelScope.launch { RetroPlayer.isPlaying.collect { _isPlaying.value = it } }
        viewModelScope.launch {
            RetroPlayer.currentId.collect { id ->
                val t = _tracks.value.firstOrNull { it.id == id } ?: return@collect
                if (_selectedTrack.value?.id != t.id) {
                    _selectedTrack.value = t
                    _nowPlaying.value = _nowPlaying.value?.copy(title = t.title)
                    _nowPlaying.value?.let { NowPlayingStatus.listening(t.title, it.artist) }
                }
            }
        }
    }

    fun updateNowPlaying(state: NowPlayingState) {
        _nowPlaying.value = state
        NowPlayingStatus.listening(state.title, state.artist)   // profile status: "🎵 Listening to …"
    }

    fun updateTracks(tracks: List<AlbumTrack>) {
        _tracks.value = tracks
        if (_selectedTrack.value == null && tracks.isNotEmpty()) {
            _selectedTrack.value = tracks.first()
        }
    }

    fun selectTrack(track: AlbumTrack) {
        _selectedTrack.value = track
        _nowPlaying.value = _nowPlaying.value?.copy(title = track.title)
        val queue = _tracks.value.ifEmpty { listOf(track) }
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val np = _nowPlaying.value
        RetroPlayer.playQueue(
            tracks = queue,
            startIndex = index,
            artist = np?.artist.orEmpty(),
            albumTitle = null,
            artwork = np?.coverUrl ?: RetroPlayer.resourceUri(np?.coverResId)
        )
    }

    fun playPause() {
        if (RetroPlayer.isPlaying.value) RetroPlayer.pause()
        else _selectedTrack.value?.let { selectTrack(it) }
    }

    fun skipNext() = RetroPlayer.next()

    fun skipPrevious() = RetroPlayer.previous()

    /** Called when the app leaves the screen; kept for compatibility (music now keeps playing). */
    fun pauseForBackground() { }

    fun clear() {
        RetroPlayer.stop()
        _nowPlaying.value = null
        _tracks.value = emptyList()
        _selectedTrack.value = null
        _isPlaying.value = false
        webViewRef = null
    }
}
