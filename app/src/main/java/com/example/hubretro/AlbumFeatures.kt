package com.example.hubretro

import android.content.Intent
import android.net.Uri
// import android.widget.Toast // If you want to use Toasts for feedback
import androidx.compose.foundation.Image
import androidx.compose.foundation.background // Keep for placeholder Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
// import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer // Added for spacing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape // Still useful for image clipping
// Card and CardDefaults are no longer needed for AlbumListItem
// import androidx.compose.material3.Card
// import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // For clipping the image
import androidx.compose.ui.geometry.Offset // ADDED for Shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow // ADDED for Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle // ADDED to apply style with shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.HubRetroTheme
import com.example.hubretro.ui.theme.RetroFontFamily
import com.example.hubretro.ui.theme.RetroTextOffWhite
import com.example.hubretro.ui.theme.VaporwavePink // Ensure this is imported and correct

// --- Data Structures ---
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val coverImageResId: Int? = null,
    val coverImageUrl: String? = null,
    val year: Int? = null,
    val webPlaybackUrl: String? = null
)

// --- Sample Data (Using your existing sample data) ---
val sampleAlbums = listOf(
    Album(
        id = "album1",
        title = "Gunbound",
        artist = "Synth Rider",
        coverImageResId = R.drawable.ostcover1,
        year = 1984,
        webPlaybackUrl = "https://archive.org/details/gunbound-soundtrack"
    ),
    Album(
        id = "album2",
        title = "Pokemon Diamond & Pearl",
        artist = "Grid Runner",
        coverImageResId = R.drawable.ostcover2,
        year = 1988,
        webPlaybackUrl = "https://archive.org/details/pkmn-dppt-soundtrack"
    ),
    Album(
        id = "album3",
        title = "The Legend of Zelda: The Wind Waker",
        artist = "Chrome Catalyst",
        coverImageResId = R.drawable.ostcover3,
        year = 1991,
        webPlaybackUrl = "https://archive.org/details/the-legend-of-zelda-the-wind-waker-ost"
    ),
    Album(
        id = "album4",
        title = "Undertale",
        artist = "Vector Voyager",
        coverImageResId = R.drawable.ostcover4,
        year = 1986,
        webPlaybackUrl = "https://archive.org/details/undertaleost_202004"
    ),
    Album(
        id = "album5",
        title = "Lego Harry Potter Years 1-4",
        artist = "Bit Shifter",
        coverImageResId = R.drawable.ostcover5,
        year = 1982,
        webPlaybackUrl = "https://archive.org/details/lego-harry-potter-years-1-4"
    ),
    Album(
        id = "album6",
        title = "Final Fantasy VII",
        artist = "Analog Hero",
        coverImageResId = R.drawable.ostcover6,
        year = 1987,
        webPlaybackUrl = "https://archive.org/details/final_fantasy_vii_soundtrack"
    ),
    Album(
        id = "album7",
        title = "The Sims",
        artist = "Digital Nomad",
        coverImageResId = R.drawable.ostcover7,
        year = 1985,
        webPlaybackUrl = "https://archive.org/details/simsmusic"
    )
)

// --- Composable for a Single Album Item (IMAGE AND TITLE ONLY, WITH SHADOW ON TITLE) ---
@Composable
fun AlbumListItem(
    album: Album,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(vertical = 4.dp) // Spacing for the item within its grid cell boundaries
            .clickable { onAlbumClick(album) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Album Cover Image
        if (album.coverImageResId != null) {
            Image(
                painter = painterResource(id = album.coverImageResId),
                contentDescription = "${album.title} cover art",
                contentScale = ContentScale.Crop, // Or ContentScale.Fit if you prefer
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp) // Your desired image height
                    .clip(RoundedCornerShape(6.dp)) // Slightly more rounded corners for the image
            )
        } else {
            // Placeholder for Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF3A3A3A), RoundedCornerShape(6.dp)) // Darker placeholder
                    .clip(RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "ART",
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Spacer between image and title
        Spacer(modifier = Modifier.height(8.dp))

        // Album Title with Shadow
        Text(
            text = album.title,
            style = TextStyle( // Apply style here
                fontFamily = FontFamily.SansSerif, // Consider RetroFontFamily if it fits the style
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = RetroTextOffWhite,         // Use your theme's off-white for consistency
                textAlign = TextAlign.Center,
                shadow = Shadow( // Add shadow configuration
                    color = VaporwavePink.copy(alpha = 0.7f), // Use your VaporwavePink
                    offset = Offset(x = 2f, y = 2f),      // Adjust offset as needed
                    blurRadius = 4f                         // Adjust blur radius as needed
                )
            ),
            maxLines = 2,                      // Allow up to two lines
            overflow = TextOverflow.Ellipsis,  // Add ellipsis if text is too long
            modifier = Modifier
                .padding(horizontal = 4.dp) // Padding for the text within the column
                .fillMaxWidth()
        )
    }
}


// --- Composable for the Albums Screen (remains the same as your current version) ---
@Composable
fun AlbumsScreen(
    albums: List<Album> = sampleAlbums,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "ALBUMS",
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(color = VaporwavePink.copy(alpha = 0.7f), offset = Offset(x = 3f, y = 3f), blurRadius = 5f),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .padding(top = 24.dp, bottom = 16.dp)
                .fillMaxWidth()
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp), // Increased padding around the grid
            verticalArrangement = Arrangement.spacedBy(12.dp), // Increased spacing between rows
            horizontalArrangement = Arrangement.spacedBy(12.dp), // Increased spacing between columns
            modifier = Modifier.fillMaxSize()
        ) {
            items(albums, key = { album -> album.id }) { album ->
                AlbumListItem(
                    album = album,
                    onAlbumClick = { selectedAlbum ->
                        selectedAlbum.webPlaybackUrl?.let { url ->
                            if (url.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    println("Could not launch web intent for ${selectedAlbum.title}: ${e.message}")
                                    // Optionally: Toast.makeText(context, "Cannot open link.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                println("Album ${selectedAlbum.title} has a blank URL.")
                                // Optionally: Toast.makeText(context, "${selectedAlbum.title} has a blank link.", Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
                            println("Album ${selectedAlbum.title} has no webPlaybackUrl.")
                            // Optionally: Toast.makeText(context, "${selectedAlbum.title} has no web link.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

// --- Previews ---

@Preview(showBackground = true, backgroundColor = 0xFF121212) // Dark background for preview
@Composable
fun AlbumListItemPreview_TransparentBg() { // Renamed preview
    HubRetroTheme {
        Box(
            modifier = Modifier
                .width(180.dp)
                .background(Color(0xFF121212)) // Simulate app's dark background
                .padding(8.dp)
        ) {
            if (sampleAlbums.isNotEmpty()) {
                AlbumListItem(
                    album = sampleAlbums.first().copy(title = "Title With Shadow"), // Updated preview text
                    onAlbumClick = {}
                )
            } else {
                Text("No sample albums for preview.", color = Color.Red)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212) // Dark background for preview
@Composable
fun AlbumsScreenPreview_TransparentItems() { // Renamed preview
    HubRetroTheme {
        // The AlbumsScreen will be transparent against the Preview's background
        AlbumsScreen(albums = sampleAlbums)
    }
}
