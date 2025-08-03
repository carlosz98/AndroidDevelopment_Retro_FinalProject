package com.example.hubretro



import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import kotlinx.coroutines.delay

const val SPLASH_DELAY_MS = 14500L // Adjust as needed (e.g., length of your GIF)

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val context = LocalContext.current

    // ImageLoader for Coil to handle GIFs
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    LaunchedEffect(Unit) {
        delay(SPLASH_DELAY_MS) // Wait for the duration of your animation/GIF
        onTimeout()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                R.drawable.bootup, // Your GIF resource
                imageLoader = imageLoader
            ),
            contentDescription = "Boot Animation",
            modifier = Modifier.fillMaxSize(), // Or adjust size as needed
            contentScale = ContentScale.Crop  // Or Fit, FillBounds, etc.
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(onTimeout = {})
}
