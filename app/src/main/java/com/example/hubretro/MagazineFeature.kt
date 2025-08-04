package com.example.hubretro

// Core Compose imports
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Your project's specific theme imports
import com.example.hubretro.ui.theme.HubRetroTheme
import com.example.hubretro.ui.theme.RetroFontFamily
import com.example.hubretro.ui.theme.RetroTextOffWhite
import com.example.hubretro.ui.theme.VaporwavePink

// --- NEW IMPORTS FOR WEB LINK ---
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext


// 1. MODIFIED Data Class for Magazine Cover
data class MagazineCover(
    val id: String,
    val title: String,
    val coverImageResId: Int? = null,
    val coverImageUrl: String? = null,
    val webUrl: String? = null // <<< ADDED for web link
)

// Define a list of your drawable resource IDs for all unique covers
val uniqueCoverResourceIds = listOf(
    R.drawable.cover1, R.drawable.cover2, R.drawable.cover3,
    R.drawable.cover4, R.drawable.cover5, R.drawable.cover6,
    R.drawable.cover7, R.drawable.cover8, R.drawable.cover9
)

// --- ADDED: Web URLs ---

val sampleMagazineWebUrls = listOf(
    "https://archive.org/details/GameProApril2004",       // Example for Magazine 1
    "https://archive.org/details/video-game-magazines/Eletronic%20Gaming%20Monthly/Electronic%20Gaming%20Monthly%20Issue%2012%20%28July%201990%29/",         // Example for Magazine 2
    "https://archive.org/details/creativecomputing",     // Example for Magazine 3
    "https://archive.org/details/video-game-magazines/Electronic%20Gaming%20Monthly%20Issue%205%20%28December%201989%29.cbr.rar/",                              // Example for Magazine 4 (First website)
    "https://archive.org/details/video-game-magazines/Eletronic%20Gaming%20Monthly/Electronic%20Gaming%20Monthly%20Issue%204%20%28November%201989%29/",                            // Example for Magazine 5
    "https://archive.org/details/GameProSeptember2005",                     // Example for Magazine 6
    "https://archive.org/details/GamePro_Issue_122_September_1999",                        // Example for Magazine 7
    "https://archive.org/details/GamePro_Issue_103_February_1998",                    // Example for Magazine 8
    "https://archive.org/details/GamePro_Issue_105_April_1998/mode/2up"                            // Example for Magazine 9
)

// 2. MODIFIED Sample Data for Magazine Covers to include webUrl
val sampleMagazineCovers = List(9) { i ->
    MagazineCover(
        id = (i + 1).toString(),
        title = "Retro Magazine ${i + 1}",
        coverImageResId = if (i < uniqueCoverResourceIds.size) uniqueCoverResourceIds[i] else R.drawable.cover1, // Fallback to cover1 if not enough unique IDs
        webUrl = if (i < sampleMagazineWebUrls.size) sampleMagazineWebUrls[i] else null // Assign the URL
    )
}


// 3. Composable for a Single Magazine Cover Item
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


// 4. MODIFIED Composable for the Main Magazines Screen with Shelves
@Composable
fun MagazinesScreen(
    magazines: List<MagazineCover> = sampleMagazineCovers,
    // The onMagazineClick parameter is now handled internally by MagazinesScreen
    // So we can remove it from the function signature if it's not used by the caller for other purposes.
    // For simplicity, I'll keep it for now in case you want to add other click logic later
    // from the calling site, but the web opening is handled here.

    modifier: Modifier = Modifier
) {
    val context = LocalContext.current // Get context for opening URL

    val magazinesPerShelf = 3
    val shelvesContent = magazines.take(magazinesPerShelf * 3).chunked(magazinesPerShelf)

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
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 16.dp)
                    .fillMaxWidth()
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
                        onMagazineClick = { magazine -> // This is the click handler
                            magazine.webUrl?.let { url ->
                                if (url.isNotBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Handle potential errors
                                        println("Error opening URL '$url': ${e.message}")
                                        Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    println("Magazine '${magazine.title}' has an empty URL.")
                                    Toast.makeText(context, "Link not available for this magazine.", Toast.LENGTH_SHORT).show()
                                }
                            } ?: run {
                                println("Magazine '${magazine.title}' has no URL defined.")
                                Toast.makeText(context, "No link defined for this magazine.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        magazinesPerShelf = magazinesPerShelf
                    )
                }
            }
        }
    }
}

// 5. Composable for a single Shelf Row (no changes needed, it just propagates the click)
@Composable
fun ShelfRow(
    magazinesOnShelf: List<MagazineCover>,
    shelfImageResId: Int,
    onMagazineClick: (MagazineCover) -> Unit, // This lambda is now more specific
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
                    onClick = { onMagazineClick(magazine) }, // Call the passed lambda
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


// 6. Preview Composable for MagazinesScreen
@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun MagazinesScreenPreview() {
    HubRetroTheme {
        // The preview won't actually open a browser.
        // Clicks in preview will try to execute the lambda defined in MagazinesScreen,
        // which would then try to get LocalContext.current. This might show a placeholder
        // or a benign error in preview mode if it can't fully resolve context for an Intent.
        // The Toast messages might also not appear in all preview environments.
        MagazinesScreen(
            magazines = sampleMagazineCovers
            // No need to pass onMagazineClick here as the preview calls MagazinesScreen directly
            // and the web opening logic is self-contained within MagazinesScreen.
        )
    }
}
