package com.example.hubretro // Or your actual package name

import androidx.annotation.DrawableRes // For imageResId type safety
import androidx.compose.animation.core.copy
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.* // Import your theme colors and fonts

// Your GIF/image file in res/drawable. Example: welcome.gif -> R.drawable.welcome
val WELCOME_IMAGE_RESOURCE_ID = R.drawable.welcome

// Placeholder drawable resources for the feature cards - REPLACE THESE
// Add appropriate images to your res/drawable folder
val ALBUMS_CARD_IMAGE = R.drawable.ostcover6 // e.g., an icon or image for albums
val MAGAZINES_CARD_IMAGE = R.drawable.cover1 // e.g., an icon or image for magazines
val ARTICLES_CARD_IMAGE = R.drawable.article1 // e.g., an icon or image for articles


@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToAlbums: () -> Unit,
    onNavigateToMagazines: () -> Unit,
    onNavigateToArticles: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            //.background(Color.Black) // Assuming a black background for the whole screen
            .padding(bottom = 16.dp)
    ) {
        // 1. "HOME" Title with Shadow
        Text(
            text = "HOME",
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
            modifier = Modifier
                .padding(top = 24.dp, bottom = 20.dp)
                .fillMaxWidth()
        )

        // 2. "WELCOME" Section
        WelcomeSection(
            imageModel = WELCOME_IMAGE_RESOURCE_ID,
            title = "WELCOME",
            description = "Dive into the digital past with RetroHub! Explore curated collections of classic game soundtracks, vintage tech magazines, and insightful articles celebrating the golden era of computing and gaming. Let the nostalgia begin!"
        )

        Spacer(modifier = Modifier.height(32.dp)) // More space before feature cards

        // 3. Feature Navigation Cards
        FeatureNavigationCard(
            title = "ALBUMS",
            description = "Groove to the classics. Soundtracks from legendary games await your ears.",
            imageResId = ALBUMS_CARD_IMAGE,
            buttonText = "TAKE ME THERE",
            onButtonClick = onNavigateToAlbums,
            gradientColors = listOf(VaporwavePink, VaporwaveBlue.copy(alpha = 0.7f))
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeatureNavigationCard(
            title = "MAGAZINES",
            description = "Flip through history. Vintage tech and gaming magazines, digitized for you.",
            imageResId = MAGAZINES_CARD_IMAGE,
            buttonText = "TAKE ME THERE",
            onButtonClick = onNavigateToMagazines,
            gradientColors = listOf(VaporwavePurple, VaporwaveCyan.copy(alpha = 0.7f))
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeatureNavigationCard(
            title = "ARTICLES",
            description = "Read insightful retrospectives and analyses on the golden age of digital.",
            imageResId = ARTICLES_CARD_IMAGE,
            buttonText = "TAKE ME THERE",
            onButtonClick = onNavigateToArticles,
            gradientColors = listOf(SynthwaveOrange, VaporwavePink.copy(alpha = 0.7f))
        )
    }
}

@Composable
fun WelcomeSection(
    imageModel: Any,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val imageShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(180.dp)
                .clip(imageShape)
                .border(
                    BorderStroke(width = 2.dp, color = VaporwavePink),
                    shape = imageShape
                )
                .background(Color.DarkGray)
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = VaporwavePink.copy(alpha = 0.6f),
                    offset = Offset(x = 2f, y = 2f),
                    blurRadius = 4f
                )
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = description,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite.copy(alpha = 0.85f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}


@Composable
fun FeatureNavigationCard(
    title: String,
    description: String,
    @DrawableRes imageResId: Int,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> // For the card background gradient
) {
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(170.dp) // Adjust height as needed
            .clip(cardShape)
            .background(Brush.horizontalGradient(colors = gradientColors)) // Gradient Background
            .border(BorderStroke(1.dp, RetroTextOffWhite.copy(alpha = 0.5f)), cardShape) // Subtle border
            .clickable(onClick = onButtonClick) // Make the whole card clickable
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column for Text and Button
            Column(
                modifier = Modifier
                    .weight(1f) // Takes up available space pushing image to the side
                    .padding(end = 12.dp), // Space between text column and image
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start // Align text to the start
            ) {
                Column { // Group title and description
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite,
                            fontSize = 20.sp, // Slightly smaller than Welcome title
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow( // Subtle shadow for card title
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(x = 1f, y = 1f),
                                blurRadius = 2f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        style = TextStyle(
                            fontFamily = RetroFontFamily, // Or a more readable sans-serif
                            color = RetroTextOffWhite.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        maxLines = 3, // Limit description lines
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.weight(1f)) // Pushes button to the bottom if there's space

                Button(
                    onClick = onButtonClick,
                    shape = CircleShape, // Rounded button like the example
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetroTextOffWhite, // Off-white button
                        contentColor = gradientColors.firstOrNull() ?: VaporwavePink // Text color from gradient
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontFamily = RetroFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Right: Image
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = title, // Decorative
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp) // Adjust size as needed
                    .clip(RoundedCornerShape(12.dp)) // Rounded image corners
                    .border(1.dp, RetroTextOffWhite.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            )
        }
    }
}


// --- Previews ---

@Preview(showBackground = true, backgroundColor = 0xFF000000) // Black background for preview
@Composable
fun HomeScreenPreview_WithNavCards() {
    HubRetroTheme {
        HomeScreen(
            onNavigateToAlbums = {},
            onNavigateToMagazines = {},
            onNavigateToArticles = {}
        )
    }
}


@Preview(showBackground = true, widthDp = 380, heightDp = 200)
@Composable
fun FeatureNavigationCardPreview() {
    HubRetroTheme {
        Box(modifier = Modifier.background(Color.Black).padding(16.dp)) {
            FeatureNavigationCard(
                title = "ALBUMS",
                description = "Groove to the classics. Soundtracks from legendary games await your ears.",
                imageResId = ALBUMS_CARD_IMAGE, // Use placeholder
                buttonText = "TAKE ME THERE",
                onButtonClick = {},
                gradientColors = listOf(VaporwavePink, VaporwaveBlue.copy(alpha = 0.7f))
            )
        }
    }
}
