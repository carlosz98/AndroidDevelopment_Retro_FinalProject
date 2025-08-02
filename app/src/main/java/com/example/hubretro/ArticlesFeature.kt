package com.example.hubretro


import androidx.compose.animation.animateContentSize
// --- ADD ANIMATION IMPORTS ---
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
// --- END ANIMATION IMPORTS ---
import androidx.compose.foundation.Image
import androidx.compose.foundation.background // <<< MAKE SURE THIS IMPORT IS PRESENT
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.*

// 1. Data Class (Unchanged)
data class ArticleItem(
    val id: String,
    val title: String,
    val snippet: String,
    val fullContent: String,
    val date: String? = null,
    val imageResId: Int? = null
)

// 2. Sample Article Data (Unchanged)
val sampleArticles = listOf(
    ArticleItem(
        id = "1",
        title = "The Rise of Retro Gaming in 2024",
        snippet = "An exploration into the resurgence of classic video games and their impact on modern culture.",
        fullContent = "The year 2024 has seen an unprecedented boom in the popularity of retro gaming. From dedicated conventions drawing massive crowds to the skyrocketing prices of vintage cartridges, the nostalgia wave is stronger than ever. This article delves into the psychological drivers behind this phenomenon, the community keeping these classics alive, and how retro aesthetics are influencing a new generation of game developers. We'll also look at the challenges of hardware preservation and the legal gray areas of emulation.",
        date = "Oct 26, 2023",
        imageResId = R.drawable.article1
    ),
    ArticleItem(
        id = "2",
        title = "Vaporwave Aesthetics: Beyond the Music",
        snippet = "Delving into the visual elements of vaporwave and its influence on digital art and fashion.",
        fullContent = "Vaporwave is more than just a musical microgenre; it's a sprawling internet-birthed aesthetic that continues to mutate and inspire. Characterized by its use of 80s and 90s commercial imagery, early internet designs, classical statues, and a distinctive color palette, vaporwave visuals evoke a sense of melancholic nostalgia for a future that never quite arrived. This piece explores its key visual motifs, its critiques of consumer capitalism, and its lasting impact on contemporary digital art, graphic design, and even mainstream fashion trends.",
        date = "Oct 15, 2023",
        imageResId = R.drawable.article2
    ),
    ArticleItem(
        id = "example_no_image",
        title = "Article Without an Image",
        snippet = "This article demonstrates how cards look when no image is provided.",
        fullContent = "Even without a captivating header image, the structure of an article card needs to be clear and inviting. The typography, spacing, and the initial snippet of text play crucial roles in drawing the reader in. This particular article serves as a live example of how content is presented when an image resource ID is null. We focus on the text hierarchy and the call to action for further reading. This is the rest of the content that would be shown when expanded.",
        date = "Oct 10, 2023",
        imageResId = R.drawable.article4
    ),
    ArticleItem(
        id = "5",
        title = "Building Your Own Retro PC: A Guide",
        snippet = "Tips and tricks for sourcing parts and assembling a personal computer with a vintage feel.",
        fullContent = "There's a unique satisfaction in piecing together a computer that boots up to a classic DOS prompt or runs Windows 98 games flawlessly. This guide will walk you through the essentials of building your own retro PC. We cover sourcing reliable vintage parts like motherboards and sound cards, compatibility considerations, finding appropriate chassis, and the software side of installing old operating systems and drivers. Get ready to dive into the rewarding world of beige boxes and floppy disks!",
        date = "Aug 28, 2023",
        imageResId = R.drawable.article3
    )
)

// 3. ArticleCard Composable (MODIFIED for background and animated shadow)
@Composable
fun ArticleCard(article: ArticleItem, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }

    // --- Animation Setup for Shadow (Unchanged from your current code) ---
    val infiniteTransition = rememberInfiniteTransition(label = "shadow_wiggle_transition")

    val shadowOffsetX by infiniteTransition.animateFloat(
        initialValue = 4.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadowOffsetX"
    )

    val shadowOffsetY by infiniteTransition.animateFloat(
        initialValue = 4.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadowOffsetY"
    )
    // --- End of Animation Setup ---

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ArticleCardDarkGrey) // <<< ADDED BACKGROUND COLOR
            .border(width = 2.dp, color = RetroBorderColor)
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
    ) {
        article.imageResId?.let { imageRes ->
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = article.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )
            Divider(
                color = RetroBorderColor,
                thickness = 2.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = article.title.uppercase(),
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    shadow = Shadow(
                        color = VaporwavePink,
                        offset = Offset(x = shadowOffsetX, y = shadowOffsetY),
                        blurRadius = 0.5f
                    )
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isExpanded) article.fullContent else article.snippet,
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite, // Check contrast against ArticleCardDarkGrey
                fontSize = 14.sp,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isExpanded) "READ LESS..." else "READ MORE...",
                fontFamily = RetroFontFamily,
                color = VaporwavePink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )

            article.date?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Published: $it",
                    fontFamily = RetroFontFamily,
                    color = Color.Gray, // Consider changing for better contrast on dark grey
                    fontSize = 12.sp
                )
            }
        }
    }
}


// 4. ArticlesScreen Composable (Unchanged)
@Composable
fun ArticlesScreen(
    modifier: Modifier = Modifier,
    articles: List<ArticleItem> = sampleArticles
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp) // This padding is for the overall screen content
        // If ArticlesScreen had its own background, it would be set here
    ) {
        Text(
            text = "LATEST ARTICLES",
            fontFamily = RetroFontFamily,
            color = RetroTextOffWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp), // Space between cards
            contentPadding = PaddingValues(bottom = 16.dp) // Padding at the end of the list
        ) {
            items(articles) { article ->
                ArticleCard(article = article) // ArticleCard will now have its own dark grey bg
            }
        }
    }
}

// 5. Previews (Unchanged - ArticleCardPreview will now reflect the new background)
@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D) // Outer background for the preview area
@Composable
fun ArticleCardPreview() {
    HubRetroTheme {
        Box(Modifier.background(RetroDarkBlue).padding(16.dp)) { // Simulates screen padding around a card
            ArticleCard(
                article = ArticleItem(
                    id = "prev1",
                    title = "Preview Article With Image",
                    snippet = "This is a snippet of the article content...",
                    fullContent = "This is the full content of the preview article, which would be longer and shown when the card is expanded. It allows us to see how the expanded state looks in the preview environment.",
                    date = "Jan 01, 2024",
                    imageResId = R.drawable.article1
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D) // Overall screen background for preview
@Composable
fun ArticlesScreenPreview() {
    HubRetroTheme {
        // ArticlesScreen itself doesn't define an explicit background here,
        // so it might take from the theme or the Box.
        // The cards *within* ArticlesScreen will get their new background.
        Box(Modifier.background(RetroDarkBlue)) {
            ArticlesScreen(articles = sampleArticles)
        }
    }
}
