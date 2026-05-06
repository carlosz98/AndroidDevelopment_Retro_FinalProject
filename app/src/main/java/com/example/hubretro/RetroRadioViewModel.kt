package com.example.hubretro

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RadioStation(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val streamUrl: String,
    val color: androidx.compose.ui.graphics.Color
)

val retroRadioStations = listOf(
    RadioStation(
        id = "chiptune",
        name = "Chiptune FM",
        description = "8-bit & 16-bit classics",
        emoji = "🕹️",
        streamUrl = "https://stream.zeno.fm/0r0xa792kwzuv",
        color = androidx.compose.ui.graphics.Color(0xFFE91E63)
    ),
    RadioStation(
        id = "gamewave",
        name = "GameWave",
        description = "Video game music 24/7",
        emoji = "🎮",
        streamUrl = "https://stream.zeno.fm/f3wvbbqmdg8uv",
        color = androidx.compose.ui.graphics.Color(0xFF9C27B0)
    ),
    RadioStation(
        id = "retro8bit",
        name = "8-Bit Radio",
        description = "Pure chiptune beats",
        emoji = "👾",
        streamUrl = "https://stream.zeno.fm/4d1d3bvkqzzuv",
        color = androidx.compose.ui.graphics.Color(0xFF2196F3)
    ),
    RadioStation(
        id = "vgm",
        name = "VGM Radio",
        description = "Video game music classics",
        emoji = "🏆",
        streamUrl = "https://stream.zeno.fm/yn65m8h9k4zuv",
        color = androidx.compose.ui.graphics.Color(0xFFFF9800)
    ),
    RadioStation(
        id = "rpg",
        name = "RPG Quest",
        description = "Epic RPG soundtracks",
        emoji = "⚔️",
        streamUrl = "https://stream.zeno.fm/4d1d3bvkqzzuv",
        color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
    )
)

data class RadioState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentStation: RadioStation? = null,
    val error: String? = null,
    val isExpanded: Boolean = false
)

class RetroRadioViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "RetroRadioViewModel"
    private var mediaPlayer: MediaPlayer? = null

    private val _radioState = MutableStateFlow(RadioState())
    val radioState: StateFlow<RadioState> = _radioState.asStateFlow()

    fun toggleExpanded() {
        _radioState.value = _radioState.value.copy(
            isExpanded = !_radioState.value.isExpanded
        )
    }

    fun collapse() {
        _radioState.value = _radioState.value.copy(isExpanded = false)
    }

    fun playStation(station: RadioStation) {
        viewModelScope.launch {
            try {
                if (_radioState.value.currentStation?.id == station.id) {
                    if (_radioState.value.isPlaying) pause() else resume()
                    return@launch
                }
                stopAndRelease()
                _radioState.value = _radioState.value.copy(
                    isLoading = true,
                    currentStation = station,
                    isPlaying = false,
                    error = null
                )
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(station.streamUrl)
                    setOnPreparedListener {
                        it.start()
                        _radioState.value = _radioState.value.copy(
                            isPlaying = true,
                            isLoading = false,
                            error = null
                        )
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                        _radioState.value = _radioState.value.copy(
                            isPlaying = false,
                            isLoading = false,
                            error = "Stream unavailable — try another station"
                        )
                        true
                    }
                    setOnCompletionListener {
                        _radioState.value = _radioState.value.copy(isPlaying = false)
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                Log.e(TAG, "Error playing station: ${e.message}")
                _radioState.value = _radioState.value.copy(
                    isPlaying = false,
                    isLoading = false,
                    error = "Failed to load stream"
                )
            }
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _radioState.value = _radioState.value.copy(isPlaying = false)
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            _radioState.value = _radioState.value.copy(isPlaying = true)
        } catch (e: Exception) {
            _radioState.value.currentStation?.let { playStation(it) }
        }
    }

    fun stop() {
        stopAndRelease()
        _radioState.value = RadioState()
    }

    private fun stopAndRelease() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player: ${e.message}")
        }
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAndRelease()
    }
}