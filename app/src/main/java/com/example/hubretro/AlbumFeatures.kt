package com.example.hubretro

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.HubRetroTheme
import com.example.hubretro.ui.theme.RetroDarkPurple
import com.example.hubretro.ui.theme.RetroFontFamily
import com.example.hubretro.ui.theme.RetroTextOffWhite
import com.example.hubretro.ui.theme.SynthwaveOrange
import com.example.hubretro.ui.theme.VaporwaveCyan
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

// --- Sample Community Albums ---
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

// --- Community Album Item ---
@Composable
fun AlbumListItem(
    album: Album,
    onAlbumClick: (Album) -> Unit,
    favoritesViewModel: FavoritesViewModel? = null,
    modifier: Modifier = Modifier
) {
    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState()
        ?: remember { mutableStateOf(emptySet<String>()) })
    val isBookmarked = favoriteIds.contains(album.id)

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clickable { onAlbumClick(album) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                if (album.coverImageResId != null) {
                    Image(
                        painter = painterResource(id = album.coverImageResId),
                        contentDescription = "${album.title} cover art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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

                // Bookmark button on cover
                if (favoritesViewModel != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                    ) {
                        IconButton(
                            onClick = {
                                favoritesViewModel.toggleFavorite(album.toFavoriteItem())
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked)
                                    Icons.Filled.Bookmark
                                else
                                    Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isBookmarked)
                                    "Remove bookmark"
                                else
                                    "Add bookmark",
                                tint = if (isBookmarked) VaporwavePink
                                else RetroTextOffWhite.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = album.title,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
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
                    fontFamily = RetroFontFamily,
                    fontSize = 10.sp,
                    color = VaporwaveCyan.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth()
            )

            album.year?.let { year ->
                Text(
                    text = year.toString(),
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        fontSize = 10.sp,
                        color = RetroTextOffWhite.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

// --- Archive Album Item ---
@Composable
fun ArchiveAlbumItem(
    item: ArchiveItem,
    onClick: () -> Unit,
    favoritesViewModel: FavoritesViewModel? = null,
    modifier: Modifier = Modifier
) {
    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState()
        ?: remember { mutableStateOf(emptySet<String>()) })
    val isBookmarked = favoriteIds.contains(item.id)

    Column(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF3A3A3A))
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Archive badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(
                        VaporwaveCyan.copy(alpha = 0.85f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ARCHIVE",
                    fontFamily = RetroFontFamily,
                    color = Color.Black,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bookmark button
            if (favoritesViewModel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    IconButton(
                        onClick = {
                            favoritesViewModel.toggleFavorite(item.toFavoriteItem())
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked)
                                Icons.Filled.Bookmark
                            else
                                Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked)
                                "Remove bookmark"
                            else
                                "Add bookmark",
                            tint = if (isBookmarked) VaporwavePink
                            else RetroTextOffWhite.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = RetroTextOffWhite,
                textAlign = TextAlign.Center,
                shadow = Shadow(
                    color = VaporwaveCyan.copy(alpha = 0.7f),
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

        item.creator?.let { creator ->
            Text(
                text = creator,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    fontSize = 10.sp,
                    color = VaporwavePink.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth()
            )
        }

        item.year?.let { year ->
            Text(
                text = year,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    fontSize = 10.sp,
                    color = RetroTextOffWhite.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth()
            )
        }
    }
}

// --- Section Header ---
@Composable
fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = RetroFontFamily,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Divider(
            modifier = Modifier.weight(1f),
            color = color.copy(alpha = 0.3f)
        )
    }
}

// --- Albums Screen ---
@Composable
fun AlbumsScreen(
    modifier: Modifier = Modifier,
    contentViewModel: ContentViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val albumsState by contentViewModel.albumsState.collectAsState()

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var lastSearched by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(600)
        if (searchQuery != lastSearched) {
            lastSearched = searchQuery
            contentViewModel.fetchAlbums(searchQuery)
        }
    }

    val filteredCommunityAlbums = remember(searchQuery) {
        if (searchQuery.isBlank()) sampleAlbums
        else sampleAlbums.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        // --- Title Row ---
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
                        contentViewModel.fetchAlbums()
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

        // --- Search Bar ---
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
                        IconButton(onClick = {
                            searchQuery = ""
                            contentViewModel.fetchAlbums()
                        }) {
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
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
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

        // --- Content ---
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Community Section
            item {
                SectionHeader(title = "COMMUNITY", color = VaporwavePink)
            }

            if (filteredCommunityAlbums.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No community albums found",
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(
                    filteredCommunityAlbums.chunked(2),
                    key = { chunk -> "community_${chunk.first().id}" }
                ) { chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        chunk.forEach { album ->
                            AlbumListItem(
                                album = album,
                                onAlbumClick = { selectedAlbum ->
                                    selectedAlbum.webPlaybackUrl?.let { url ->
                                        if (url.isNotBlank()) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            try { context.startActivity(intent) } catch (e: Exception) { }
                                        }
                                    }
                                },
                                favoritesViewModel = favoritesViewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Archive Section
            item {
                SectionHeader(title = "INTERNET ARCHIVE", color = VaporwaveCyan)
            }

            when (val state = albumsState) {
                is ContentState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = VaporwaveCyan,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading soundtracks...",
                                    fontFamily = RetroFontFamily,
                                    color = RetroTextOffWhite.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                is ContentState.Error -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.message,
                                    fontFamily = RetroFontFamily,
                                    color = SynthwaveOrange,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { contentViewModel.fetchAlbums() },
                                    colors = ButtonDefaults.buttonColors(containerColor = VaporwaveCyan),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        "RETRY",
                                        fontFamily = RetroFontFamily,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                is ContentState.Success -> {
                    items(
                        state.items.chunked(2),
                        key = { chunk -> "archive_${chunk.first().id}" }
                    ) { chunk ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunk.forEach { item ->
                                ArchiveAlbumItem(
                                    item = item,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.webUrl))
                                        try { context.startActivity(intent) } catch (e: Exception) { }
                                    },
                                    favoritesViewModel = favoritesViewModel,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                else -> { }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun AlbumsScreenPreview() {
    HubRetroTheme {
        AlbumsScreen()
    }
}