package com.example.hubretro

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.hubretro.ui.theme.VaporwavePink // <<< ADD THIS IMPORT

// Import screens
import com.example.hubretro.MagazinesScreen
import com.example.hubretro.ArticlesScreen
import com.example.hubretro.AlbumsScreen
import com.example.hubretro.HomeScreen



// Define data structure for Top Action Items
data class TopActionItem(
    val label: String,
    val route: String // route can still be useful if you switch to Nav Component later
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
                    // Ensure R.drawable.my_retro_background exists
                    Image(
                        painter = painterResource(id = R.drawable.my_retro_background),
                        contentDescription = "Retro background image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Scaffold(
                        containerColor = Color.Transparent, // Keep scaffold background transparent
                        topBar = {
                            Box(modifier = Modifier.padding(top = 55.dp)) { // Your desired top padding
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
                        // Pass the innerPadding to the content Box
                        Box(modifier = Modifier.padding(innerPadding)) {
                            Log.d("ScreenSelection", "Rendering for: $selectedActionLabel")
                            when (selectedActionLabel) {
                                "ARTICLES" -> {
                                    ArticlesScreen()
                                }
                                "HOME" -> {
                                    HomeScreen(
                                        onNavigateToAlbums = { selectedActionLabel = "ALBUMS" },
                                        onNavigateToMagazines = { selectedActionLabel = "MAGAZINES" },
                                        onNavigateToArticles = { selectedActionLabel = "ARTICLES" }
                                    )
                                }
                                "MAGAZINES" -> {
                                    MagazinesScreen()
                                }
                                "ALBUMS" -> {
                                    AlbumsScreen()
                                }
                                else -> { // Default to HOME
                                    HomeScreen(
                                        onNavigateToAlbums = { selectedActionLabel = "ALBUMS" },
                                        onNavigateToMagazines = { selectedActionLabel = "MAGAZINES" },
                                        onNavigateToArticles = { selectedActionLabel = "ARTICLES" }
                                    )
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
    textColor: Color = Color.Yellow // Note: This textColor default is still Yellow
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

@Composable
fun RetroAppBar(
    title: String,
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
        actionItems.forEach { item ->
            Text(
                text = item.label,

                color = if (item.label == selectedItemLabel) VaporwavePink else Color.White,
                fontWeight = if (item.label == selectedItemLabel) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clickable { onActionItemClick(item) }
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D)
@Composable
fun DefaultPreview() {
    HubRetroTheme {
        var selectedPreviewLabel by remember { mutableStateOf("HOME") }

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
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                when (selectedPreviewLabel) {
                    "ARTICLES" -> ArticlesScreen()
                    "MAGAZINES" -> MagazinesScreen()
                    "ALBUMS" -> AlbumsScreen()
                    "HOME" -> HomeScreen(
                        onNavigateToAlbums = { selectedPreviewLabel = "ALBUMS" },
                        onNavigateToMagazines = { selectedPreviewLabel = "MAGAZINES" },
                        onNavigateToArticles = { selectedPreviewLabel = "ARTICLES" }
                    )
                    else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MainTitle(text = "HubRetro Preview ($selectedPreviewLabel)")
                    }
                }
            }
        }
    }
}
