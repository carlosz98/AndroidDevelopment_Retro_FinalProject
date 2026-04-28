package com.example.hubretro

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.HubRetroTheme
import com.example.hubretro.ui.theme.RetroFontFamily
import com.example.hubretro.ui.theme.RetroTextOffWhite
import com.example.hubretro.ui.theme.VaporwavePink
import com.example.hubretro.ui.theme.VaporwaveCyan
import com.example.hubretro.ui.theme.RetroDarkPurple

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

// --- Single Album Item ---
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

        Text(
            text = album.artist,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                color = VaporwaveCyan.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
        )
    }
}

// --- Albums Screen ---
@Composable
fun AlbumsScreen(
    albums: List<Album> = sampleAlbums,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredAlbums = remember(searchQuery, albums) {
        if (searchQuery.isBlank()) albums
        else albums.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        // --- Title Row with Search Icon ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.width(40.dp))

            Text(
                text = "ALBUMS",
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = VaporwavePink.copy(alpha = 0.7f),
                        offset = Offset(x = 3f, y = 3f),
                        blurRadius = 5f
                    ),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    searchVisible = !searchVisible
                    if (!searchVisible) {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (searchVisible) "Close search" else "Search albums",
                    tint = if (searchVisible) VaporwavePink else RetroTextOffWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // --- Animated Search Bar ---
        AnimatedVisibility(
            visible = searchVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search albums or artists...",
                        fontFamily = RetroFontFamily,
                        fontSize = 12.sp,
                        color = RetroTextOffWhite.copy(alpha = 0.4f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = VaporwavePink,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = RetroTextOffWhite.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                textStyle = TextStyle(
                    fontFamily = RetroFontFamily,
                    fontSize = 13.sp,
                    color = RetroTextOffWhite
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VaporwavePink,
                    unfocusedBorderColor = RetroTextOffWhite.copy(alpha = 0.3f),
                    focusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                    unfocusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                    cursorColor = VaporwavePink
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // --- No results ---
        if (searchQuery.isNotBlank() && filteredAlbums.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No albums found for \"$searchQuery\"",
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }

        // --- Albums Grid ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredAlbums, key = { album -> album.id }) { album ->
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
                            }
                        }
                    }
                )
            }
        }
    }
}

// --- Preview ---
@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun AlbumsScreenPreview() {
    HubRetroTheme {
        AlbumsScreen(albums = sampleAlbums)
    }
}