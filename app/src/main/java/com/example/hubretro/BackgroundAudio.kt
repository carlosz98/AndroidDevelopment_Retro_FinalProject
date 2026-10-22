package com.example.hubretro

import android.media.MediaPlayer
import android.webkit.WebView
import java.util.Collections
import java.util.WeakHashMap

// ═══════════════════════════════════════════════════════════════════════════════
// BackgroundAudio — keeps the app quiet when it isn't on screen.
//
// Every WebView (album player, Twitch/TikTok streams, magazines, articles, events)
// and every short music-preview MediaPlayer registers itself here with track(this).
// MainActivity then:
//   • onStop    → pauseAll()       (home button, recents, screen off, another app)
//   • onStart   → resumeWebViews() (pages run again; music stays paused)
//   • onDestroy → releaseAll()     (app closed: back from the main screen / swiped away)
//
// References are weak, so tracking never keeps a closed screen in memory.
// The radio and the album "Now Playing" bar are paused through their ViewModels
// so their buttons show the right state.
// ═══════════════════════════════════════════════════════════════════════════════
object BackgroundAudio {

    private val webViews: MutableSet<WebView> =
        Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<WebView, Boolean>()))
    private val players: MutableSet<MediaPlayer> =
        Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<MediaPlayer, Boolean>()))

    private const val PAUSE_MEDIA_JS =
        "document.querySelectorAll('audio,video').forEach(function(m){ try { m.pause(); } catch(e){} });"

    fun track(webView: WebView) { webViews.add(webView) }

    fun track(player: MediaPlayer) { players.add(player) }

    private fun webViewSnapshot(): List<WebView> = synchronized(webViews) { webViews.toList() }
    private fun playerSnapshot(): List<MediaPlayer> = synchronized(players) { players.toList() }

    /** Silence every page and preview. Safe to call any number of times. */
    fun pauseAll() {
        webViewSnapshot().forEach { wv ->
            runCatching {
                wv.evaluateJavascript(PAUSE_MEDIA_JS, null)
                wv.onPause()   // pauses media, plugins and animations in this WebView
            }
        }
        playerSnapshot().forEach { mp ->
            // A released player throws IllegalStateException; that just means it's already silent.
            runCatching { if (mp.isPlaying) mp.pause() }
        }
    }

    /** Let pages run again after the app comes back. Doesn't restart any music. */
    fun resumeWebViews() {
        webViewSnapshot().forEach { wv -> runCatching { wv.onResume() } }
    }

    /** App is closing: stop and free everything that was tracked. */
    fun releaseAll() {
        webViewSnapshot().forEach { wv ->
            runCatching {
                wv.evaluateJavascript(PAUSE_MEDIA_JS, null)
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.onPause()
                (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                wv.destroy()
            }
        }
        webViews.clear()
        playerSnapshot().forEach { mp -> runCatching { mp.stop() }; runCatching { mp.release() } }
        players.clear()
    }
}
