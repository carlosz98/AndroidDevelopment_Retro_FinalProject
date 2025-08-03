package com.example.hubretro

// import com.example.hubretro.ArticlesScreen // This import is already here, that's good!
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement // Added for RetroAppBar placeholder
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row // Added for RetroAppBar placeholder
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth // Added for RetroAppBar placeholder
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
import androidx.compose.foundation.clickable // Added for RetroAppBar placeholder

// Import your new MagazinesScreen
import com.example.hubretro.MagazinesScreen // <<< ADD THIS IMPORT
// Import ArticlesScreen (it was commented out, but you are using it)
import com.example.hubretro.ArticlesScreen // <<< ENSURE THIS IMPORT IS PRESENT AND CORRECT


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
                                RetroAppBar( // Assuming RetroAppBar is defined elsewhere or you have the placeholder below
                                    title = "", // title in RetroAppBar is not used in your placeholder, so this is fine
                                    actionItems = topAppBarActionItems,
                                    selectedItemLabel = selectedActionLabel,
                                    onActionItemClick = { actionItem ->
                                        Log.d("AppBarClick", "Clicked: ${actionItem.label}") // Keep this Log
                                        selectedActionLabel = actionItem.label
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            Log.d("ScreenSelection", "Rendering for: $selectedActionLabel")
                            when (selectedActionLabel) {
                                "ARTICLES" -> {
                                    ArticlesScreen()
                                }
                                "HOME" -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MainTitle(text = "HubRetro")
                                    }
                                }
                                "MAGAZINES" -> {
                                    // --- MODIFIED HERE ---
                                    MagazinesScreen() // No longer takes onMagazineClick
                                }
                                "ALBUMS" -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Albums Page Coming Soon!", color = Color.White, fontFamily = RetroFontFamily, fontSize = 20.sp)
                                    }
                                }
                                else -> {
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

// Placeholder for RetroAppBar if not defined elsewhere (you likely have this in another file)
// If you have it defined elsewhere, you can remove this.
@Composable
fun RetroAppBar(
    title: String, // This parameter is present but not used in the Row below
    actionItems: List<TopActionItem>,
    selectedItemLabel: String,
    onActionItemClick: (TopActionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // If title was meant to be displayed, you'd add a Text(title) here
        actionItems.forEach { item ->
            Text(
                text = item.label,
                color = if (item.label == selectedItemLabel) Color.Yellow else Color.White,
                fontWeight = if (item.label == selectedItemLabel) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clickable { onActionItemClick(item) }
            )
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D)
@Composable
fun DefaultPreview() {
    HubRetroTheme {
        var selectedPreviewLabel by remember { mutableStateOf("MAGAZINES") }

        Scaffold(
            topBar = {
                Box(modifier = Modifier.padding(top = 16.dp)) { // Adjusted padding for preview to match behavior
                    RetroAppBar(
                        title = "",
                        actionItems = topAppBarActionItems,
                        selectedItemLabel = selectedPreviewLabel,
                        onActionItemClick = { item -> selectedPreviewLabel = item.label }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                when (selectedPreviewLabel) {
                    "ARTICLES" -> ArticlesScreen()
                    "MAGAZINES" -> {
                        // --- MODIFIED HERE ---
                        MagazinesScreen() // No longer takes onMagazineClick
                    }
                    "HOME" -> Box(modifier=Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MainTitle(text = "HubRetro Preview (HOME)") }
                    "ALBUMS" -> Box(modifier=Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Albums Page Preview", color = Color.White) }
                    else -> Box(modifier=Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MainTitle(text = "HubRetro Preview ($selectedPreviewLabel)") }
                }
            }
        }
    }
}

