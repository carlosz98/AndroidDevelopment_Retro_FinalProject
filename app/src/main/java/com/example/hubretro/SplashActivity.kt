// SplashActivity.kt
package com.example.hubretro

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.hubretro.ui.theme.HubRetroTheme // Your app's theme

@SuppressLint("CustomSplashScreen") // If you're not using the new SplashScreen API here
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HubRetroTheme { // Apply your app's theme
                SplashScreen {
                    // This will be called after SPLASH_DELAY_MS
                    navigateToMain()
                }
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish() // Finish SplashActivity so user can't navigate back to it
    }
}
