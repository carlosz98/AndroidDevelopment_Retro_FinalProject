package com.example.hubretro // Your package declaration

// Core Compose imports
import androidx.compose.foundation.Image
// import androidx.compose.foundation.background // Not strictly needed for this change
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
// import androidx.compose.foundation.layout.height // <<< MIGHT ADD this for shelf image (Kept as is from your base)
import androidx.compose.foundation.layout.padding
// LazyGrid imports are replaced by LazyColumn imports
// import androidx.compose.foundation.lazy.grid.GridCells
// import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
// import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
// Material 3 imports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
// Runtime imports
import androidx.compose.runtime.Composable
// UI imports
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset // <<< --- ADDED IMPORT for Shadow ---
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow // <<< --- ADDED IMPORT for Shadow ---
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle // <<< --- ADDED IMPORT for TextStyle ---
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // <<< --- Ensure this import is present (it was already)
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Your project's specific theme imports (ensure these paths are correct for your project)
import com.example.hubretro.ui.theme.HubRetroTheme
import com.example.hubretro.ui.theme.RetroFontFamily
import com.example.hubretro.ui.theme.RetroTextOffWhite
import com.example.hubretro.ui.theme.VaporwavePink // <<< --- ENSURE THIS IMPORT (or define color locally) ---


// 1. Data Class for Magazine Cover (remains the same)
data class MagazineCover(
    val id: String,
    val title: String,
    val coverImageResId: Int? = null, // For local drawable (like a placeholder)
    val coverImageUrl: String? = null // For network images from Internet Archive later
)

// 2. Sample Data for Magazine Covers (remains the same, ensure it has 9 items for 3x3)
val sampleMagazineCovers = List(9) { i ->
    MagazineCover(
        id = (i + 1).toString(),
        title = "Retro Magazine ${i + 1}",
        coverImageResId = when (i % 3) {
            0 -> R.drawable.cover1
            1 -> R.drawable.cover2
            else -> R.drawable.cover3
        }
    )
}

// 3. Composable for a Single Magazine Cover Item (remains the same)
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
            } else if (magazine.coverImageUrl != null) {
                Text(
                    text = "Loading: ${magazine.title.take(20)}...",
                    color = RetroTextOffWhite,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
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


// 4. Composable for the Main Magazines Screen with Shelves (MODIFIED for TextStyle textAlign)
@Composable
fun MagazinesScreen(
    magazines: List<MagazineCover> = sampleMagazineCovers,
    onMagazineClick: (MagazineCover) -> Unit,
    modifier: Modifier = Modifier
) {
    val magazinesPerShelf = 3
    val shelvesContent = magazines.take((3 * magazinesPerShelf)).chunked(magazinesPerShelf)

    // Optional: Define VaporwavePink here if not using from theme, for direct use
    // val vaporwavePinkColorForShadow = Color(0xFFF955C7) // Example: replace F955C7 with your hex

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "VIRTUAL MAGAZINES",
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = VaporwavePink,
                        offset = Offset(x = 4f, y = 4f),
                        blurRadius = 8f
                    ),
                    textAlign = TextAlign.Center // <<< --- ADDED THIS LINE ---
                ),
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 16.dp)
                    .align(Alignment.CenterHorizontally) // This still ensures the Text composable itself is centered
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp),
                verticalArrangement = Arrangement.spacedBy(0.1.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(shelvesContent) { _, shelfMagazines ->
                    ShelfRow(
                        magazinesOnShelf = shelfMagazines,
                        shelfImageResId = R.drawable.shelf,
                        onMagazineClick = onMagazineClick,
                        magazinesPerShelf = magazinesPerShelf
                    )
                }
            }
        }
    }
}

// 5. MODIFIED Composable for a single Shelf Row (Kept as per your base code)
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
    // Note: Your base code had an extra closing brace here for ShelfRow. I am keeping it
    // as you provided, but typically this would be a syntax error.
}


// 6. Preview Composable for MagazinesScreen (no change needed from your last version)
@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun MagazinesScreenPreview() {
    HubRetroTheme {
        MagazinesScreen(
            magazines = sampleMagazineCovers.take(9),
            onMagazineClick = { magazine ->
                println("Preview: Clicked on magazine - ${magazine.title}")
            }
        )
    }
}
