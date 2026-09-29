package com.example.hubretro

import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.IconButton
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*

@Composable
fun FavoritesScreen(
    favoritesViewModel: FavoritesViewModel,
    modifier: Modifier = Modifier
) {
    val favorites by favoritesViewModel.favorites.collectAsState()
    val isLoading by favoritesViewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // Group by category
    val albums = favorites.filter { it.category == "ALBUM" }
    val magazines = favorites.filter { it.category == "MAGAZINE" }
    val articles = favorites.filter { it.category == "ARTICLE" }

    Box(modifier = modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                NowLoadingIndicator(label = "READING MEMORY CARD")
            }
        } else if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                NoSaveDataState(subtitle = "Bookmark albums, magazines and articles to save them to your memory card!")
            }
        } else {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Albums section
        if (albums.isNotEmpty()) {
            item {
                FavoritesSectionHeader(
                    title = "ALBUMS",
                    icon = Icons.Filled.MusicNote,
                    color = VaporwavePink,
                    count = albums.size
                )
            }
            items(albums, key = { "fav_album_${it.id}" }) { item ->
                FavoriteCard(
                    item = item,
                    accentColor = VaporwavePink,
                    onRemove = { favoritesViewModel.removeFavorite(item.id) },
                    onClick = {
                        if (item.webUrl.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.webUrl))
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        }
                    }
                )
            }
        }

        // Magazines section
        if (magazines.isNotEmpty()) {
            item {
                FavoritesSectionHeader(
                    title = "MAGAZINES",
                    icon = Icons.Filled.MenuBook,
                    color = VaporwavePurple,
                    count = magazines.size
                )
            }
            items(magazines, key = { "fav_mag_${it.id}" }) { item ->
                FavoriteCard(
                    item = item,
                    accentColor = VaporwavePurple,
                    onRemove = { favoritesViewModel.removeFavorite(item.id) },
                    onClick = {
                        if (item.webUrl.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.webUrl))
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        }
                    }
                )
            }
        }

        // Articles section
        if (articles.isNotEmpty()) {
            item {
                FavoritesSectionHeader(
                    title = "ARTICLES",
                    icon = Icons.Filled.Article,
                    color = VaporwaveCyan,
                    count = articles.size
                )
            }
            items(articles, key = { "fav_art_${it.id}" }) { item ->
                FavoriteCard(
                    item = item,
                    accentColor = VaporwaveCyan,
                    onRemove = { favoritesViewModel.removeFavorite(item.id) },
                    onClick = {
                        if (item.webUrl.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.webUrl))
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        }
                    }
                )
            }
        }
    }
    } // close else
    } // close root Box
} // close FavoritesScreen

@Composable
fun BookmarkButton(
    isBookmarked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.size(32.dp)
    ) {
        Icon(
            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
            tint = if (isBookmarked) VaporwavePink else ScrapbookDark.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun FavoritesSectionHeader(
    title: String,
    icon: ImageVector,
    color: Color,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = color.copy(alpha = 0.5f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        androidx.compose.material3.Divider(
            modifier = Modifier.weight(1f),
            color = color.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.2f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun FavoriteCard(
    item: FavoriteItem,
    accentColor: Color,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glowAlpha by rememberGlowPhase(0.35f)
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "fPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "fShOff")
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }

    Box(modifier = modifier.fillMaxWidth().offset(y = pressAnim.dp)) {
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp)
            .clip(RoundedCornerShape(10.dp)).background(ScrapbookDark.copy(alpha = 0.15f)))
        Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(10.dp))
            .background(CGreen.copy(alpha = glowAlpha * 0.2f)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(
                    width = 2.dp,
                    color = ScrapbookDark,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { pressed = true; onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ComicGlassBg)
        ) {
            if (item.thumbnailUrl != null) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.halftoneReveal(item.thumbnailUrl).fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = ScrapbookDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            item.creator?.let { creator ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = creator,
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = accentColor.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item.year?.let { year ->
                Text(
                    text = year,
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Remove bookmark button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
                .border(1.5.dp, ScrapbookDark, CircleShape)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = "Remove from favorites",
                tint = ScrapbookDark,
                modifier = Modifier.size(18.dp)
            )
        }
        }
    }
}