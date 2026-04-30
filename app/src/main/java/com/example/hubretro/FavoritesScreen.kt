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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = VaporwavePink,
                modifier = Modifier.size(40.dp)
            )
        }
        return
    }

    if (favorites.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = RetroTextOffWhite.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "NO FAVORITES YET",
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bookmark albums, magazines and articles to save them here!",
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
}

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
            tint = if (isBookmarked) VaporwavePink else RetroTextOffWhite.copy(alpha = 0.6f),
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RetroDarkPurple.copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A3A))
        ) {
            if (item.thumbnailUrl != null) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
                    color = RetroTextOffWhite,
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
                        color = RetroTextOffWhite.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Remove bookmark button
        androidx.compose.material3.IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = "Remove from favorites",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}