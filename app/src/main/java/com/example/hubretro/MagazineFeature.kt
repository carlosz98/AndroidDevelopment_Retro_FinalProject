package com.example.hubretro

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.HubRetroTheme
import com.example.hubretro.ui.theme.RetroFontFamily
import com.example.hubretro.ui.theme.RetroTextOffWhite
import com.example.hubretro.ui.theme.VaporwavePink
import com.example.hubretro.ui.theme.VaporwaveCyan
import com.example.hubretro.ui.theme.RetroDarkPurple

// 1. Data Class for Magazine Cover
data class MagazineCover(
    val id: String,
    val title: String,
    val coverImageResId: Int? = null,
    val coverImageUrl: String? = null,
    val webUrl: String? = null
)

// Drawable resource IDs
val uniqueCoverResourceIds = listOf(
    R.drawable.cover1, R.drawable.cover2, R.drawable.cover3,
    R.drawable.cover4, R.drawable.cover5, R.drawable.cover6,
    R.drawable.cover7, R.drawable.cover8, R.drawable.cover9
)

// Web URLs
val sampleMagazineWebUrls = listOf(
    "https://archive.org/details/GameProApril2004",
    "https://archive.org/details/video-game-magazines/Eletronic%20Gaming%20Monthly/Electronic%20Gaming%20Monthly%20Issue%2012%20%28July%201990%29/",
    "https://archive.org/details/creativecomputing",
    "https://archive.org/details/video-game-magazines/Electronic%20Gaming%20Monthly%20Issue%205%20%28December%201989%29.cbr.rar/",
    "https://archive.org/details/video-game-magazines/Eletronic%20Gaming%20Monthly/Electronic%20Gaming%20Monthly%20Issue%204%20%28November%201989%29/",
    "https://archive.org/details/GameProSeptember2005",
    "https://archive.org/details/GamePro_Issue_122_September_1999",
    "https://archive.org/details/GamePro_Issue_103_February_1998",
    "https://archive.org/details/GamePro_Issue_105_April_1998/mode/2up"
)

// Sample magazine titles for search filtering
val sampleMagazineTitles = listOf(
    "GamePro April 2004",
    "Electronic Gaming Monthly Issue 12",
    "Creative Computing",
    "Electronic Gaming Monthly Issue 5",
    "Electronic Gaming Monthly Issue 4",
    "GamePro September 2005",
    "GamePro Issue 122",
    "GamePro Issue 103",
    "GamePro Issue 105"
)

// Sample Data
val sampleMagazineCovers = List(9) { i ->
    MagazineCover(
        id = (i + 1).toString(),
        title = if (i < sampleMagazineTitles.size) sampleMagazineTitles[i] else "Retro Magazine ${i + 1}",
        coverImageResId = if (i < uniqueCoverResourceIds.size) uniqueCoverResourceIds[i] else R.drawable.cover1,
        webUrl = if (i < sampleMagazineWebUrls.size) sampleMagazineWebUrls[i] else null
    )
}

// 2. Single Magazine Cover Item
@Composable
fun MagazineCoverItem(
    magazine: MagazineCover,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.7f))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (magazine.coverImageResId != null) {
                Image(
                    painter = painterResource(id = magazine.coverImageResId),
                    contentDescription = magazine.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = magazine.title,
                    color = RetroTextOffWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

// 3. Main Magazines Screen
@Composable
fun MagazinesScreen(
    magazines: List<MagazineCover> = sampleMagazineCovers,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Filter magazines based on search query
    val filteredMagazines = remember(searchQuery, magazines) {
        if (searchQuery.isBlank()) magazines
        else magazines.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }
    }

    // When searching show a grid, otherwise show the shelves
    val isSearching = searchQuery.isNotBlank()

    val magazinesPerShelf = 3
    val shelvesContent = filteredMagazines
        .take(magazinesPerShelf * 3)
        .chunked(magazinesPerShelf)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- Title Row with Search Icon ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.size(40.dp))

                Text(
                    text = "VIRTUAL MAGAZINES",
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = VaporwavePink,
                            offset = Offset(x = 4f, y = 4f),
                            blurRadius = 8f
                        ),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Search toggle icon
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
                        contentDescription = if (searchVisible) "Close search" else "Search magazines",
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
                            "Search magazines...",
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

            // --- No results message ---
            if (isSearching && filteredMagazines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No magazines found for \"$searchQuery\"",
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            // --- Content: Grid when searching, Shelves when not ---
            if (isSearching) {
                // Show as a simple grid when searching
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredMagazines, key = { it.id }) { magazine ->
                        MagazineCoverItem(
                            magazine = magazine,
                            onClick = {
                                magazine.webUrl?.let { url ->
                                    if (url.isNotBlank()) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                // Show on shelves when not searching
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                    verticalArrangement = Arrangement.spacedBy(0.1.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(shelvesContent) { _, shelfMagazines ->
                        ShelfRow(
                            magazinesOnShelf = shelfMagazines,
                            shelfImageResId = R.drawable.shelf,
                            onMagazineClick = { magazine ->
                                magazine.webUrl?.let { url ->
                                    if (url.isNotBlank()) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Link not available.", Toast.LENGTH_SHORT).show()
                                    }
                                } ?: Toast.makeText(context, "No link defined.", Toast.LENGTH_SHORT).show()
                            },
                            magazinesPerShelf = magazinesPerShelf
                        )
                    }
                }
            }
        }
    }
}

// 4. Shelf Row
@Composable
fun ShelfRow(
    magazinesOnShelf: List<MagazineCover>,
    shelfImageResId: Int,
    onMagazineClick: (MagazineCover) -> Unit,
    magazinesPerShelf: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = shelfImageResId),
            contentDescription = "Magazine Shelf",
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize(),
            contentScale = ContentScale.FillBounds
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 140.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            magazinesOnShelf.forEach { magazine ->
                MagazineCoverItem(
                    magazine = magazine,
                    onClick = { onMagazineClick(magazine) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (magazinesOnShelf.size < magazinesPerShelf) {
                for (i in 0 until (magazinesPerShelf - magazinesOnShelf.size)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// 5. Preview
@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun MagazinesScreenPreview() {
    HubRetroTheme {
        MagazinesScreen(magazines = sampleMagazineCovers)
    }
}