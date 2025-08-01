package com.example.hubretro


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider // <<< ADD THIS IMPORT
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import com.example.hubretro.R
import com.example.hubretro.ui.theme.*

// 1. Data Class for an Article
data class ArticleItem(
    val id: String,
    val title: String,
    val snippet: String,
    val date: String? = null,
    val imageResId: Int? = null
)

// 2. Sample Article Data (NEEDS YOUR ACTUAL IMAGE RESOURCES)
val sampleArticles = listOf(
    ArticleItem(
        id = "1",
        title = "The Rise of Retro Gaming in 2024",
        snippet = "An exploration into the resurgence of classic video games and their impact on modern culture.",
        date = "Oct 26, 2023",
        imageResId = R.drawable.article1 // <<< EXAMPLE: REPLACE with your actual image
    ),
    ArticleItem(
        id = "2",
        title = "Vaporwave Aesthetics: Beyond the Music",
        snippet = "Delving into the visual elements of vaporwave and its influence on digital art and fashion.",
        date = "Oct 15, 2023",
        imageResId = R.drawable.article2 // <<< EXAMPLE: REPLACE
    ),
    ArticleItem(
        id = "example_no_image",
        title = "Article Without an Image",
        snippet = "This article demonstrates how cards look when no image is provided.",
        date = "Oct 10, 2023",
        imageResId = R.drawable.article4
    ),
    ArticleItem(
        id = "5",
        title = "Building Your Own Retro PC: A Guide",
        snippet = "Tips and tricks for sourcing parts and assembling a personal computer with a vintage feel.",
        date = "Aug 28, 2023",
        imageResId = R.drawable.article3 // <<< EXAMPLE: REPLACE
    )
)

// 3. ArticleCard Composable (CORRECTED)
@Composable
fun ArticleCard(article: ArticleItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = RetroBorderColor)
    ) {
        article.imageResId?.let { imageRes ->
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = article.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f), // Keep aspect ratio
                contentScale = ContentScale.Crop
            )
            // Add a Divider below the image
            Divider(
                color = RetroBorderColor, // Color of the line
                thickness = 2.dp,        // Thickness of the line
                modifier = Modifier.fillMaxWidth() // Ensure it spans the width if needed within padding
                // If you want padding on the sides of the divider,
                // apply it here, e.g., .padding(horizontal = 16.dp)
                // but for a full-width line within the card, this is fine.
            )
        }

        // Column for text content with its own padding
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
                text = article.snippet,
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 14.sp,
                lineHeight = 18.sp
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


// 4. ArticlesScreen Composable (No changes needed)
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

// 5. Previews
@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D)
@Composable
fun ArticleCardPreview() {
    HubRetroTheme {
        Box(Modifier.background(RetroDarkBlue)) {
            ArticleCard(
                article = ArticleItem(
                    id = "prev1",
                    title = "Preview Article With Image",
                    snippet = "This is a snippet of the article content to see how it looks in the card with an image.",
                    date = "Jan 01, 2024",
                    imageResId = R.drawable.article1 // <<< EXAMPLE: Replace with an actual drawable for preview
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
            ArticlesScreen(articles = sampleArticles)
        }
    }
}
