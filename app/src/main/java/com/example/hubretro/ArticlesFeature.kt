package com.example.hubretro


import androidx.compose.animation.animateContentSize // <<< ADD IMPORT
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable // <<< ADD IMPORT
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // <<< ADD IMPORT (ensure it's here)
import androidx.compose.runtime.mutableStateOf // <<< ADD IMPORT (ensure it's here)
import androidx.compose.runtime.remember // <<< ADD IMPORT (ensure it's here)
import androidx.compose.runtime.setValue // <<< ADD IMPORT (ensure it's here)
import androidx.compose.ui.Alignment // <<< ADD IMPORT
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.text.style.TextOverflow // Optional: For snippet truncation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import com.example.hubretro.R
import com.example.hubretro.ui.theme.*

// 1. Data Class for an Article (MODIFIED)
data class ArticleItem(
    val id: String,
    val title: String,
    val snippet: String,
    val fullContent: String, // <<< ADDED for complete article text
    val date: String? = null,
    val imageResId: Int? = null
)

// 2. Sample Article Data (MODIFIED - ADD fullContent to your articles)
val sampleArticles = listOf(
    ArticleItem(
        id = "1",
        title = "The Rise of Retro Gaming in 2024",
        snippet = "An exploration into the resurgence of classic video games and their impact on modern culture.",
        fullContent = "The year 2024 has seen an unprecedented boom in the popularity of retro gaming. From dedicated conventions drawing massive crowds to the skyrocketing prices of vintage cartridges, the nostalgia wave is stronger than ever. This article delves into the psychological drivers behind this phenomenon, the community keeping these classics alive, and how retro aesthetics are influencing a new generation of game developers. We'll also look at the challenges of hardware preservation and the legal gray areas of emulation.", // <<< ADDED
        date = "Oct 26, 2023",
        imageResId = R.drawable.article1
    ),
    ArticleItem(
        id = "2",
        title = "Vaporwave Aesthetics: Beyond the Music",
        snippet = "Delving into the visual elements of vaporwave and its influence on digital art and fashion.",
        fullContent = "Vaporwave is more than just a musical microgenre; it's a sprawling internet-birthed aesthetic that continues to mutate and inspire. Characterized by its use of 80s and 90s commercial imagery, early internet designs, classical statues, and a distinctive color palette, vaporwave visuals evoke a sense of melancholic nostalgia for a future that never quite arrived. This piece explores its key visual motifs, its critiques of consumer capitalism, and its lasting impact on contemporary digital art, graphic design, and even mainstream fashion trends.", // <<< ADDED
        date = "Oct 15, 2023",
        imageResId = R.drawable.article2
    ),
    ArticleItem(
        id = "example_no_image",
        title = "Article Without an Image",
        snippet = "This article demonstrates how cards look when no image is provided.",
        fullContent = "Even without a captivating header image, the structure of an article card needs to be clear and inviting. The typography, spacing, and the initial snippet of text play crucial roles in drawing the reader in. This particular article serves as a live example of how content is presented when an image resource ID is null. We focus on the text hierarchy and the call to action for further reading. This is the rest of the content that would be shown when expanded.", // <<< ADDED
        date = "Oct 10, 2023",
        imageResId = R.drawable.article4 // Assuming this was meant to be null as per previous example, or replace if article4 exists.
        // For "no image" example, imageResId should ideally be null.
    ),
    ArticleItem(
        id = "5",
        title = "Building Your Own Retro PC: A Guide",
        snippet = "Tips and tricks for sourcing parts and assembling a personal computer with a vintage feel.",
        fullContent = "There's a unique satisfaction in piecing together a computer that boots up to a classic DOS prompt or runs Windows 98 games flawlessly. This guide will walk you through the essentials of building your own retro PC. We cover sourcing reliable vintage parts like motherboards and sound cards, compatibility considerations, finding appropriate chassis, and the software side of installing old operating systems and drivers. Get ready to dive into the rewarding world of beige boxes and floppy disks!", // <<< ADDED
        date = "Aug 28, 2023",
        imageResId = R.drawable.article3
    )
)

// 3. ArticleCard Composable (MODIFIED)
@Composable
fun ArticleCard(article: ArticleItem, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) } // State for each card

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = RetroBorderColor)
            .animateContentSize() // Smoothly animates size changes
            .clickable { isExpanded = !isExpanded } // Make the whole card clickable
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
                color = VaporwavePink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isExpanded) article.fullContent else article.snippet,
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                // Optional: For snippet view, you might want to limit lines & add ellipsis
                // maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                // overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isExpanded) "READ LESS..." else "READ MORE...",
                fontFamily = RetroFontFamily,
                color = VaporwavePink, // Use an accent color
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.End) // Align to the right
            )

            article.date?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Published: $it",
                    fontFamily = RetroFontFamily,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}


// 4. ArticlesScreen Composable (No changes needed for this feature)
@Composable
fun ArticlesScreen(
    modifier: Modifier = Modifier,
    articles: List<ArticleItem> = sampleArticles
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(articles) { article ->
                ArticleCard(article = article)
            }
        }
    }
}

// 5. Previews (Update ArticleCardPreview with fullContent)
@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D)
@Composable
fun ArticleCardPreview() {
    HubRetroTheme {
        Box(Modifier.background(RetroDarkBlue)) {
            ArticleCard(
                article = ArticleItem(
                    id = "prev1",
                    title = "Preview Article With Image",
                    snippet = "This is a snippet of the article content...",
                    fullContent = "This is the full content of the preview article, which would be longer and shown when the card is expanded. It allows us to see how the expanded state looks in the preview environment.", // <<< ADDED
                    date = "Jan 01, 2024",
                    imageResId = R.drawable.article1
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D)
@Composable
fun ArticlesScreenPreview() {
    HubRetroTheme {
        Box(Modifier.background(RetroDarkBlue)) {
            ArticlesScreen(articles = sampleArticles) // Will now use articles with fullContent
        }
    }
}

