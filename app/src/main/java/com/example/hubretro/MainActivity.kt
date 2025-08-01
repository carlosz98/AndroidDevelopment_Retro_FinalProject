package com.example.hubretro

// import com.example.hubretro.ArticlesScreen // This import is already here, that's good!
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.HubRetroTheme
import com.example.hubretro.ui.theme.RetroFontFamily

// Define data structure for Top Action Items
data class TopActionItem(
    val label: String,
    val route: String
)

val topAppBarActionItems = listOf(
    TopActionItem("HOME", "home"),
    TopActionItem("MAGAZINES", "magazines"),
    TopActionItem("ALBUMS", "albums"),
    TopActionItem("ARTICLES", "articles")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HubRetroTheme {
                var selectedActionLabel by remember { mutableStateOf("HOME") }

                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.my_retro_background),
                        contentDescription = "Retro background image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                            Box(modifier = Modifier.padding(top = 50.dp)) {
                                RetroAppBar(
                                    title = "",
                                    actionItems = topAppBarActionItems,
                                    selectedItemLabel = selectedActionLabel,
                                    onActionItemClick = { actionItem ->
                                        Log.d("AppBarClick", "Clicked: ${actionItem.label}")
                                        selectedActionLabel = actionItem.label
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        // Apply the innerPadding from Scaffold to the content area
                        Box(modifier = Modifier.padding(innerPadding)) { // <<< MODIFICATION HERE
                            // Determine which content to show based on selectedActionLabel
                            when (selectedActionLabel) {
                                "ARTICLES" -> {
                                    ArticlesScreen() // <<< DISPLAY YOUR ARTICLES SCREEN
                                }
                                "HOME" -> {
                                    // Ensure MainTitle is centered within the available space
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MainTitle(text = "HubRetro")
                                    }
                                }
                                "MAGAZINES" -> { // Placeholder for other screens
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Magazines Page Coming Soon!", color = Color.White, fontFamily = RetroFontFamily, fontSize = 20.sp)
                                    }
                                }
                                "ALBUMS" -> { // Placeholder for other screens
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Albums Page Coming Soon!", color = Color.White, fontFamily = RetroFontFamily, fontSize = 20.sp)
                                    }
                                }
                                else -> { // Default case, could also be HOME or another default screen
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MainTitle(text = "HubRetro")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainTitle(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Yellow
) {
    Text(
        text = text.uppercase(),
        color = textColor,
        fontFamily = RetroFontFamily,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D)
@Composable
fun DefaultPreview() {
    HubRetroTheme {
        var selectedPreviewLabel by remember { mutableStateOf("MAGAZINES") }

        Scaffold(
            topBar = {
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    RetroAppBar(
                        title = "",
                        actionItems = topAppBarActionItems,
                        selectedItemLabel = selectedPreviewLabel,
                        onActionItemClick = { item -> selectedPreviewLabel = item.label }
                    )
                }
            }
        ) { innerPadding ->
            // In Preview, you might want to show a specific screen or a placeholder
            // For simplicity, let's keep it showing MainTitle for now,
            // or you could introduce the 'when' logic here too if needed.
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Example of how to preview a specific screen:
                // if (selectedPreviewLabel == "ARTICLES") {
                //     ArticlesScreen() // Assuming ArticlesScreen is accessible and has its own preview data
                // } else {
                //     MainTitle(text = "HubRetro Preview ($selectedPreviewLabel)")
                // }
                MainTitle(text = "HubRetro Preview ($selectedPreviewLabel)")
            }
        }
    }
}
