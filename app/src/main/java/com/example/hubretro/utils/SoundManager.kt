package com.example.hubretro.utils // Make sure this package name matches where you created the file

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.hubretro.R // IMPORTANT: Make sure this R import is correct for your project

object SoundManager {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()

    // --- DEFINE YOUR SOUND EFFECT KEYS HERE ---
    // These keys are integers that you'll use to refer to your sounds.
    // Their actual values don't matter as much as them being unique.
    // We'll map these keys to the loaded sound IDs from SoundPool.

    // Let's use the R.raw resource IDs directly as keys for simplicity in this example,
    // but you could define arbitrary constants like const val MY_COOL_SOUND = 1
    // and then map that in initialize. For now, this is easier:

    // Example: If your sound file in res/raw is "button_click.mp3"
    // R.raw.button_click will be its resource ID.
    // Add all your sound file references here:
    val SOUND_BUTTON_PRIMARY_CLICK = R.raw.navigation_sound // Replace 'button_click_sound' with your actual file name
    val SOUND_NAVIGATION_TAP = R.raw.button_sound   // Replace 'navigation_tap_sound'
    // Add more sounds:
    // val SOUND_ERROR_BEEP = R.raw.error_beep_sound
    // val SOUND_SUCCESS_CHIME = R.raw.success_chime_sound

    fun initialize(context: Context) {
        if (soundPool != null) {
            return // Already initialized
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5) // Adjust as needed
            .setAudioAttributes(audioAttributes)
            .build()

        // --- LOAD YOUR SOUNDS ---
        // The first parameter to load is context, second is resource ID, third is priority (1 is normal)
        // Store the returned sound ID in the map, using your defined key.
        soundMap[SOUND_BUTTON_PRIMARY_CLICK] = soundPool?.load(context, SOUND_BUTTON_PRIMARY_CLICK, 1) ?: 0
        soundMap[SOUND_NAVIGATION_TAP] = soundPool?.load(context, SOUND_NAVIGATION_TAP, 1) ?: 0
        // Load other sounds:
        // soundMap[SOUND_ERROR_BEEP] = soundPool?.load(context, SOUND_ERROR_BEEP, 1) ?: 0
        // soundMap[SOUND_SUCCESS_CHIME] = soundPool?.load(context, SOUND_SUCCESS_CHIME, 1) ?: 0

        // Optional: Set a completion listener to see if sounds loaded (for debugging)
        // soundPool?.setOnLoadCompleteListener { soundPool, sampleId, status ->
        //     Log.d("SoundManager", "Sound loaded: ID $sampleId, Status $status")
        // }
    }

    fun playSound(soundKey: Int) {
        if (soundPool == null) {
            // Log.w("SoundManager", "SoundPool not initialized, cannot play sound.")
            return
        }
        soundMap[soundKey]?.let { soundId ->
            if (soundId != 0) { // 0 usually means loading failed or not loaded
                // Parameters: soundID, leftVolume, rightVolume, priority, loop (0 for no loop), rate (1.0f for normal)
                soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
            } else {
                // Log.w("SoundManager", "Sound ID for key $soundKey not found or failed to load.")
            }
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        // Log.d("SoundManager", "SoundPool released")
    }
}
