package com.example.hubretro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hubretro.ui.theme.*

// --- Data class for search results ---
data class DiscoverResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: DiscoverCategory
)

enum class DiscoverCategory(val label: String, val color: androidx.compose.ui.graphics.Color) {
    USER("USER", VaporwaveCyan),
    MAGAZINE("MAGAZINE", VaporwavePink),
    ALBUM("ALBUM", SynthwaveOrange),
    ARTICLE("ARTICLE", VaporwaveGreen)
}

@Composable
fun DiscoverScreen(
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }

    // Combine all searchable content into one list
    val allResults: List<DiscoverResult> = remember {
        // Users — will come from Firestore in the future,
        // for now we use a placeholder
        val userResults = emptyList<DiscoverResult>()

        // Magazines
        val magazineResults = sampleMagazineCovers.map {
            DiscoverResult(
                id = "mag_${it.id}",
                title = it.title,
                subtitle = "Virtual Magazine",
                category = DiscoverCategory.MAGAZINE
            )
        }

        // Albums
        val albumResults = sampleAlbums.map {
            DiscoverResult(
                id = "alb_${it.id}",
                title = it.title,
                subtitle = it.artist,
                category = DiscoverCategory.ALBUM
            )
        }

        // Articles
        val articleResults = sampleArticles.map {
            DiscoverResult(
                id = "art_${it.id}",
                title = it.title,
                subtitle = it.author ?: "Unknown author",
                category = DiscoverCategory.ARTICLE
            )
        }

        userResults + magazineResults + albumResults + articleResults
    }

    // Filter results based on search query
    val filteredResults = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allResults.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    // Group results by category
    val groupedResults = filteredResults.groupBy { it.category }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // --- Title ---
        Text(
            text = "DISCOVER",
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                shadow = Shadow(
                    color = VaporwaveCyan.copy(alpha = 0.8f),
                    offset = Offset(x = 4f, y = 4f),
                    blurRadius = 8f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Text(
            text = "Search users, magazines, albums & articles",
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        )

        // --- Search Bar (always visible on Discover) ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                hasSearched = it.isNotBlank()
            },
            placeholder = {
                Text(
                    "Search anything...",
                    fontFamily = RetroFontFamily,
                    fontSize = 12.sp,
                    color = RetroTextOffWhite.copy(alpha = 0.4f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = VaporwaveCyan,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        hasSearched = false
                        focusManager.clearFocus()
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
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            ),
            textStyle = TextStyle(
                fontFamily = RetroFontFamily,
                fontSize = 13.sp,
                color = RetroTextOffWhite
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VaporwaveCyan,
                unfocusedBorderColor = RetroTextOffWhite.copy(alpha = 0.3f),
                focusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                unfocusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                cursorColor = VaporwaveCyan
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Results ---
        when {
            // No search yet — show category hints
            !hasSearched -> {
                DiscoverHints()
            }

            // Searched but no results
            filteredResults.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🔍",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No results for \"$searchQuery\"",
                            style = TextStyle(
                                fontFamily = RetroFontFamily,
                                color = RetroTextOffWhite.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            // Show grouped results
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Render each category group
                    DiscoverCategory.values().forEach { category ->
                        val categoryResults = groupedResults[category]
                        if (!categoryResults.isNullOrEmpty()) {
                            item {
                                // Category header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.label,
                                        style = TextStyle(
                                            fontFamily = RetroFontFamily,
                                            color = category.color,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            shadow = Shadow(
                                                color = category.color.copy(alpha = 0.5f),
                                                offset = Offset(2f, 2f),
                                                blurRadius = 4f
                                            )
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Divider(
                                        modifier = Modifier.weight(1f),
                                        color = category.color.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${categoryResults.size}",
                                        style = TextStyle(
                                            fontFamily = RetroFontFamily,
                                            color = category.color.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                            items(categoryResults, key = { it.id }) { result ->
                                DiscoverResultCard(result = result)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Individual result card ---
@Composable
fun DiscoverResultCard(
    result: DiscoverResult,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RetroDarkPurple.copy(alpha = 0.7f))
            .border(
                BorderStroke(1.dp, result.category.color.copy(alpha = 0.4f)),
                RoundedCornerShape(10.dp)
            )
            .clickable { /* TODO: navigate to item */ }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category color dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(result.category.color, CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = result.subtitle,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.6f),
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Category badge
        Box(
            modifier = Modifier
                .background(
                    result.category.color.copy(alpha = 0.2f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = result.category.label,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = result.category.color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// --- Hints shown before searching ---
@Composable
fun DiscoverHints() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "WHAT CAN YOU FIND?",
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite.copy(alpha = 0.5f),
                fontSize = 11.sp,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        listOf(
            Triple("👤", "USERS", "Find and follow other retro enthusiasts"),
            Triple("📰", "MAGAZINES", "Browse classic gaming & tech magazines"),
            Triple("🎵", "ALBUMS", "Discover retro game soundtracks"),
            Triple("📝", "ARTICLES", "Read articles by the community")
        ).forEach { (emoji, title, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RetroDarkPurple.copy(alpha = 0.5f))
                    .border(
                        BorderStroke(1.dp, RetroTextOffWhite.copy(alpha = 0.1f)),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = desc,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}