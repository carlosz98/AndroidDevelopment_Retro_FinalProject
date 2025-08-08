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
// Add/Ensure these imports are present in MainActivity.kt
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.Icons // Already likely present
import androidx.compose.material.icons.filled.Menu // For the hamburger icon
import androidx.compose.material3.IconButton // For the hamburger icon button
import androidx.compose.material3.Icon // Already likely present
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
// Other imports you already have...

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
import com.example.hubretro.ProfileScreen // <<< IMPORT YOUR PROFILE SCREEN



// Define data structure for Top Action Items
data class TopActionItem(
    val label: String,
    val route: String // route can still be useful if you switch to Nav Component later
)

// In MainActivity.kt
// val drawerNavItems = listOf(
// TopActionItem("HOME", "home", Icons.Filled.Home),
// TopActionItem("MAGAZINES", "magazines", Icons.Filled.PhotoAlbum),
// TopActionItem("ALBUMS", "albums", Icons.Filled.LibraryMusic),
// TopActionItem("ARTICLES", "articles", Icons.Filled.Article),
// TopActionItem("PROFILE", "profile", Icons.Filled.AccountCircle)
// )

val drawerNavItems = listOf(
    TopActionItem("HOME", "home"),
    TopActionItem("MAGAZINES", "magazines"),
    TopActionItem("ALBUMS", "albums"),
    TopActionItem("ARTICLES", "articles"),
    TopActionItem("PROFILE", "profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HubRetroTheme {
                // 1. Setup for Navigation Drawer
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                // selectedActionLabel will now correspond to the items in drawerNavItems
                var selectedActionLabel by remember { mutableStateOf(drawerNavItems.first().label) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.my_retro_background),
                        contentDescription = "Retro background image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // 2. Wrap Scaffold with ModalNavigationDrawer
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                // ***** ADD STYLING FOR DRAWER BACKGROUND *****
                                drawerContainerColor = Color(0xFF2A2A3D).copy(alpha = 0.95f) // Your specific grey, semi-transparent
                                // Or use Color.DarkGray if #2A2A3D isn't defined as VaporwaveDarkBackground or similar
                                // drawerContainerColor = Color.DarkGray.copy(alpha = 0.9f),
                            ) {
                                Spacer(Modifier.height(20.dp)) // Padding at the top of the drawer
                                drawerNavItems.forEach { item ->
                                    NavigationDrawerItem(
                                        label = {
                                            Text(
                                                item.label,
                                                fontFamily = RetroFontFamily, // Apply your font
                                                fontSize = 18.sp // Adjust size
                                            )
                                        },
                                        selected = item.label == selectedActionLabel,
                                        onClick = {
                                            selectedActionLabel = item.label
                                            scope.launch { drawerState.close() }
                                        },
                                        // Modifier to add padding around each item
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), // Slightly adjusted vertical for item spacing
                                        // ***** ADD STYLING FOR DRAWER ITEMS *****
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedTextColor = VaporwavePink,      // Text color when selected
                                            selectedContainerColor = Color.Black.copy(alpha = 0.2f), // Subtle dark highlight for selected item's background

                                            unselectedTextColor = Color.White,        // Text color when not selected
                                            unselectedContainerColor = Color.Transparent // No specific background for unselected items
                                        )
                                    )
                                }
                                Spacer(Modifier.height(20.dp)) // Padding at the bottom
                            }
                        }
                    ) { // Content of the screen, which is your Scaffold
                        Scaffold(
                            containerColor = Color.Transparent,
                            topBar = {
                                Box(modifier = Modifier.padding(top = 55.dp)) {
                                    // 3. Update RetroAppBar call
                                    RetroAppBar(
                                        currentScreenLabel = selectedActionLabel, // Pass current label for title
                                        onNavigationIconClick = { // Lambda to open/close drawer
                                            scope.launch {
                                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                            }
                                        }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                Log.d("ScreenSelection", "Rendering for: $selectedActionLabel")
                                when (selectedActionLabel.uppercase()) {
                                    "ARTICLES" -> ArticlesScreen()
                                    "HOME" -> HomeScreen(
                                        onNavigateToAlbums = { selectedActionLabel = "ALBUMS" },
                                        onNavigateToMagazines = { selectedActionLabel = "MAGAZINES" },
                                        onNavigateToArticles = { selectedActionLabel = "ARTICLES" }
                                    )
                                    "MAGAZINES" -> MagazinesScreen()
                                    "ALBUMS" -> AlbumsScreen()
                                    "PROFILE" -> ProfileScreen()
                                    else -> {
                                        val defaultScreen = drawerNavItems.firstOrNull()
                                        if (defaultScreen != null) {
                                            selectedActionLabel = defaultScreen.label
                                            when (defaultScreen.label.uppercase()) {
                                                "HOME" -> HomeScreen(
                                                    onNavigateToAlbums = { selectedActionLabel = "ALBUMS" },
                                                    onNavigateToMagazines = { selectedActionLabel = "MAGAZINES" },
                                                    onNavigateToArticles = { selectedActionLabel = "ARTICLES" }
                                                )
                                                else -> HomeScreen(
                                                    onNavigateToAlbums = { selectedActionLabel = "ALBUMS" },
                                                    onNavigateToMagazines = { selectedActionLabel = "MAGAZINES" },
                                                    onNavigateToArticles = { selectedActionLabel = "ARTICLES" }
                                                )
                                            }
                                        } else {
                                            Text("No screens available.")
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

// Replace your existing RetroAppBar function with this one
@Composable
fun RetroAppBar(
    currentScreenLabel: String,
    onNavigationIconClick: () -> Unit,
    modifier: Modifier = Modifier // Keep modifier for flexibility
) {
    Row(
        modifier = modifier // Use the passed modifier
            .fillMaxWidth()
            // .background(Color.Red.copy(alpha = 0.2f)) // Optional: temporary background to see its bounds
            .padding(horizontal = 8.dp, vertical = 16.dp), // Adjust padding as needed
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigationIconClick) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open Navigation Menu",
                tint = Color.White // Or your desired icon color
            )
        }
        // Spacer to push the title to the center.
        // Text itself will be centered within its available space.
        Text(
            text = currentScreenLabel.uppercase(),
            color = Color.White, // Or your desired text color
            fontFamily = RetroFontFamily,
            fontSize = 20.sp, // Adjust as needed
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f) // Text takes up remaining space, helping to center it
        )
        // Optional: If you need to perfectly balance the IconButton on the left,
        // you could add a Spacer here with the same width as the IconButton,
        // but make it invisible or transparent.
        // For example: Spacer(Modifier.width(48.dp)) // IconButton is typically 48dp wide
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D)
@Composable
fun DefaultPreview() {
    HubRetroTheme {
        // The preview will show one screen at a time, based on this state.
        // Initialize with the first item from drawerNavItems.
        var selectedPreviewScreenLabel by remember { mutableStateOf(drawerNavItems.firstOrNull()?.label ?: "HOME") }

        Scaffold(
            topBar = {
                // This Box simulates the padding you have in your main app's topBar.
                // Adjust 55.dp if your main app's topBar padding is different.
                Box(modifier = Modifier.padding(top = 55.dp)) {
                    RetroAppBar(
                        currentScreenLabel = selectedPreviewScreenLabel,
                        onNavigationIconClick = {
                            // In a preview, the drawer won't actually open,
                            // but you could log something here if you want.
                            Log.d("Preview", "Navigation icon clicked. Current screen: $selectedPreviewScreenLabel")
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding) // Apply the Scaffold's inner padding
                    .fillMaxSize(),
            ) {
                // This 'when' block determines which screen to show in the preview.
                // It mirrors the logic in your main app.
                when (selectedPreviewScreenLabel.uppercase()) {
                    "ARTICLES" -> ArticlesScreen()
                    "MAGAZINES" -> MagazinesScreen()
                    "ALBUMS" -> AlbumsScreen()
                    "HOME" -> HomeScreen(
                        // For the preview, these lambdas can simply change the
                        // selectedPreviewScreenLabel to simulate navigation.
                        onNavigateToAlbums = { selectedPreviewScreenLabel = "ALBUMS" },
                        onNavigateToMagazines = { selectedPreviewScreenLabel = "MAGAZINES" },
                        onNavigateToArticles = { selectedPreviewScreenLabel = "ARTICLES" }
                    )
                    "PROFILE" -> ProfileScreen() // Assuming you have a ProfileScreen for preview
                    else -> {
                        // Fallback for any unknown label, or if drawerNavItems was empty.
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            MainTitle(text = "Preview: $selectedPreviewScreenLabel")
                        }
                    }
                }
            }
        }
    }
}

