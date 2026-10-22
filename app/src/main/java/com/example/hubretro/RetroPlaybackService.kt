package com.example.hubretro

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ═══════════════════════════════════════════════════════════════════════════════
// Background music player for Albums.
//   • RetroPlaybackService: an ExoPlayer inside a MediaSessionService, so music keeps
//     playing when you switch apps or lock the phone, with play/pause/next/previous on
//     the lock screen, in the notification shade and on Bluetooth headphones.
//   • Swiping RetroHub away from recents stops the music (onTaskRemoved).
//   • RetroPlayer: the app's handle to it (NowPlayingViewModel talks to this).
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(UnstableApi::class)
class RetroPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)   // pause when headphones are unplugged
            .build()
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        session = MediaSession.Builder(this, player).apply { openApp?.let { setSessionActivity(it) } }.build()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().also { it.setSmallIcon(R.drawable.ic_stat_retrohub) }
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    // App swiped away → stop the music and the service
    override fun onTaskRemoved(rootIntent: Intent?) {
        session?.player?.let { it.stop(); it.clearMediaItems() }
        stopSelf()
    }

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}

object RetroPlayer {
    private lateinit var appContext: Context
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _currentId = MutableStateFlow<String?>(null)
    val currentId: StateFlow<String?> = _currentId.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { _currentId.value = mediaItem?.mediaId }
    }

    fun init(context: Context) { appContext = context.applicationContext }

    private fun connect() {
        if (future != null || !::appContext.isInitialized) return
        val token = SessionToken(appContext, ComponentName(appContext, RetroPlaybackService::class.java))
        val f = MediaController.Builder(appContext, token).buildAsync()
        future = f
        f.addListener({
            try {
                val c = f.get()
                controller = c
                c.addListener(listener)
                _isPlaying.value = c.isPlaying
                _currentId.value = c.currentMediaItem?.mediaId
                pending.forEach { it(c) }
            } catch (_: Exception) { future = null }
            pending.clear()
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun withController(block: (MediaController) -> Unit) {
        controller?.let(block) ?: run { pending += block; connect() }
    }

    /** Plays [tracks] starting at [startIndex]. Keeps playing if that exact track is already loaded. */
    fun playQueue(tracks: List<AlbumTrack>, startIndex: Int, artist: String, albumTitle: String?, artwork: String?) {
        if (tracks.isEmpty()) return
        val start = startIndex.coerceIn(0, tracks.lastIndex)
        withController { c ->
            if (c.currentMediaItem?.mediaId == tracks[start].id && c.mediaItemCount == tracks.size) {
                if (!c.isPlaying) c.play(); return@withController
            }
            val art = artwork?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            val items = tracks.map { t ->
                MediaItem.Builder()
                    .setMediaId(t.id)
                    .setUri(t.playUrl)
                    .setMediaMetadata(
                        MediaMetadata.Builder().setTitle(t.title).setArtist(artist).setAlbumTitle(albumTitle).setArtworkUri(art).build()
                    ).build()
            }
            c.setMediaItems(items, start, 0L)
            c.prepare()
            c.play()
        }
    }

    fun toggle() = withController { if (it.isPlaying) it.pause() else it.play() }
    fun pause() = withController { it.pause() }
    fun next() = withController { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    fun previous() = withController { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() else it.seekTo(0L) }
    fun stop() { controller?.run { stop(); clearMediaItems() } }

    /** android.resource:// link for album art bundled in the app. */
    fun resourceUri(resId: Int?): String? =
        if (resId == null || !::appContext.isInitialized) null else "android.resource://${appContext.packageName}/$resId"
}
