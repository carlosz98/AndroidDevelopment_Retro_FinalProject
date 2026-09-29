package com.example.hubretro

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NowPlayingViewModel : ViewModel() {

    private val _nowPlaying = MutableStateFlow<NowPlayingState?>(null)
    val nowPlaying: StateFlow<NowPlayingState?> = _nowPlaying.asStateFlow()

    private val _tracks = MutableStateFlow<List<AlbumTrack>>(emptyList())
    val tracks: StateFlow<List<AlbumTrack>> = _tracks.asStateFlow()

    private val _selectedTrack = MutableStateFlow<AlbumTrack?>(null)
    val selectedTrack: StateFlow<AlbumTrack?> = _selectedTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // WebView ref — not a StateFlow since it's not serializable
    var webViewRef: WebView? = null

    fun updateNowPlaying(state: NowPlayingState) {
        _nowPlaying.value = state
        _isPlaying.value = true
    }

    fun updateTracks(tracks: List<AlbumTrack>) {
        _tracks.value = tracks
        if (_selectedTrack.value == null && tracks.isNotEmpty()) {
            _selectedTrack.value = tracks.first()
        }
    }

    fun selectTrack(track: AlbumTrack) {
        _selectedTrack.value = track
        _isPlaying.value = true
        webViewRef?.loadUrl(track.playUrl)
        _nowPlaying.value = _nowPlaying.value?.copy(title = track.title)
    }

    fun playPause() {
        val playing = !_isPlaying.value
        _isPlaying.value = playing
        if (playing) {
            _selectedTrack.value?.let { webViewRef?.loadUrl(it.playUrl) }
        } else {
            webViewRef?.evaluateJavascript(
                "document.querySelectorAll('audio,video').forEach(m => m.pause())", null
            )
        }
    }

    fun skipNext() {
        val tracks = _tracks.value
        val idx = tracks.indexOfFirst { it.id == _selectedTrack.value?.id }
        if (idx < tracks.size - 1) {
            selectTrack(tracks[idx + 1])
        }
    }

    fun skipPrevious() {
        val tracks = _tracks.value
        val idx = tracks.indexOfFirst { it.id == _selectedTrack.value?.id }
        if (idx > 0) {
            selectTrack(tracks[idx - 1])
        }
    }

    fun clear() {
        _nowPlaying.value = null
        _tracks.value = emptyList()
        _selectedTrack.value = null
        _isPlaying.value = false
        webViewRef = null
    }
}