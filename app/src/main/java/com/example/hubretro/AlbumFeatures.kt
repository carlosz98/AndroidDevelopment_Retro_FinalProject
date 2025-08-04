package com.example.hubretro

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import com.example.hubretro.ui.theme.VaporwavePink

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

// --- Sample Data ---
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

// --- Composable for a Single Album Item ---
@Composable
fun AlbumListItem(
    album: Album,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { onAlbumClick(album) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (album.coverImageResId != null) {
            Image(
                painter = painterResource(id = album.coverImageResId),
                contentDescription = "${album.title} cover art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF3A3A3A), RoundedCornerShape(6.dp))
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.title,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = RetroTextOffWhite,
                textAlign = TextAlign.Center,
                shadow = Shadow(
                    color = VaporwavePink.copy(alpha = 0.7f),
                    offset = Offset(x = 2f, y = 2f),
                    blurRadius = 4f
                )
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
        )
    }
}


// --- Composable for the Albums Screen ---
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
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                }
                            } else {
                                println("Album ${selectedAlbum.title} has a blank URL.")
                            }
                        } ?: run {
                            println("Album ${selectedAlbum.title} has no webPlaybackUrl.")
                        }
                    }
                )
            }
        }
    }
}

// --- Previews ---
@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun AlbumListItemPreview_TransparentBg() {
    HubRetroTheme {
        Box(
            modifier = Modifier
                .width(180.dp)
                .background(Color(0xFF121212))
                .padding(8.dp)
        ) {
            if (sampleAlbums.isNotEmpty()) {
                AlbumListItem(
                    album = sampleAlbums.first().copy(title = "Title With Shadow"),
                    onAlbumClick = {}
                )
            } else {
                Text("No sample albums for preview.", color = Color.Red)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun AlbumsScreenPreview_TransparentItems() {
    HubRetroTheme {
        AlbumsScreen(albums = sampleAlbums)
    }
}
