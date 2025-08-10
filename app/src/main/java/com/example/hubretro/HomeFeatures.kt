package com.example.hubretro

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.* // Make sure RetroGold is here or define it
import java.util.Calendar
import android.util.Log


// --- Constants for Image Resources ---
val WELCOME_IMAGE_RESOURCE_ID = R.drawable.welcome
val ALBUMS_CARD_IMAGE = R.drawable.ostcover6
val MAGAZINES_CARD_IMAGE = R.drawable.cover1
val ARTICLES_CARD_IMAGE = R.drawable.article1
val PROFILE_CARD_IMAGE = R.drawable.p1 // <<< MAKE SURE THIS DRAWABLE EXISTS

// --- Main Home Screen Composable ---
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToAlbums: () -> Unit,
    onNavigateToMagazines: () -> Unit,
    onNavigateToArticles: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
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

        WelcomeSection(
            imageModel = WELCOME_IMAGE_RESOURCE_ID,
            title = "WELCOME",
            description = "Dive into the digital past with RetroHub! Explore curated collections of classic game soundtracks, vintage tech magazines, and insightful articles celebrating the golden era of computing and gaming. Let the nostalgia begin!"
        )

        Spacer(modifier = Modifier.height(32.dp))

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

        // --- ADDED PROFILE CARD ---
        Spacer(modifier = Modifier.height(20.dp))

        FeatureNavigationCard(
            title = "PROFILE",
            description = "Manage your settings and view your retro journey.", // Customize this text
            imageResId = PROFILE_CARD_IMAGE, // Use the constant for your profile image
            buttonText = "VIEW PROFILE",      // Customize button text
            onButtonClick = onNavigateToProfile,
            gradientColors = listOf(RetroGold, VaporwavePink.copy(alpha = 0.6f)) // Customize colors (ensure RetroGold is defined or use other colors)
        )
        // --- END OF ADDED PROFILE CARD ---

        Spacer(modifier = Modifier.height(48.dp))

        CopyrightFooter(
            name = "Carlos Zabala",
            blogUrl = "https://charlysblog.framer.website"
        )
    }
}

// --- Welcome Section Composable ---
@Composable
fun WelcomeSection(
    imageModel: Any, // Changed from Int to Any to support AsyncImage model types
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
        ) {
            AsyncImage( // Using AsyncImage which allows for various model types (URL, R.drawable, etc.)
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

// --- Feature Navigation Card Composable ---
@Composable
fun FeatureNavigationCard(
    title: String,
    description: String,
    @DrawableRes imageResId: Int,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color>
) {
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // Added padding here for consistency if needed, or remove if parent handles it
            .height(170.dp) // Fixed height for the card
            .clip(cardShape)
            .background(Brush.horizontalGradient(colors = gradientColors))
            .border(BorderStroke(1.dp, RetroTextOffWhite.copy(alpha = 0.5f)), cardShape)
            .clickable(onClick = onButtonClick) // Make the whole card clickable
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Inner padding for content
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f) // Text content takes available space
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween, // Pushes button to bottom
                horizontalAlignment = Alignment.Start
            ) {
                Column { // Group title and description
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
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
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        maxLines = 3, // Limit description lines
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.weight(1f)) // Pushes button to the bottom if content is short

                Button(
                    onClick = onButtonClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetroTextOffWhite, // Button background
                        contentColor = gradientColors.firstOrNull() ?: VaporwavePink // Button text color from gradient
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
                    modifier = Modifier.padding(top = 8.dp) // Spacing above button
                ) {
                    Text(
                        text = buttonText,
                        fontFamily = RetroFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Image(
                painter = painterResource(id = imageResId),
                contentDescription = title, // Accessibility: describes the image
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp) // Fixed size for the image
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, RetroTextOffWhite.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            )
        }
    }
}

// --- Copyright Footer Composable ---
@Composable
fun CopyrightFooter(name: String, blogUrl: String, modifier: Modifier = Modifier) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp), // Standard padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "© $currentYear $name. All Rights Reserved.",
            style = TextStyle(
                fontFamily = RetroFontFamily,
                fontSize = 12.sp,
                color = Color.White, // Using direct Color.White for simplicity here
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(4.dp))

        val annotatedString = buildAnnotatedString {
            append("Visit my blog: ")
            pushStringAnnotation(tag = "URL", annotation = blogUrl)
            withStyle(
                style = SpanStyle(
                    color = Color.White, // Ensure text is visible
                    textDecoration = TextDecoration.Underline,
                    fontFamily = RetroFontFamily, // Consistent font
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("charlysblog.framer.website") // The visible link text
            }
            pop()
        }

        ClickableText(
            text = annotatedString,
            style = TextStyle( // General style for the ClickableText if needed
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontFamily = RetroFontFamily,
                color = Color.White // Default text color
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        try {
                            uriHandler.openUri(annotation.item)
                        } catch (e: Exception) {
                            Log.e("CopyrightFooter", "Could not open URI: ${annotation.item}", e)
                        }
                    }
            }
        )
    }
}

// --- Previews ---
@Preview(showBackground = true, backgroundColor = 0xFF000000) // Dark background for preview
@Composable
fun HomeScreenPreview_WithNavCardsAndFooter() {
    HubRetroTheme {
        // Provide dummy lambdas for preview
        HomeScreen(
            onNavigateToAlbums = { Log.d("Preview", "Navigate to Albums") },
            onNavigateToMagazines = { Log.d("Preview", "Navigate to Magazines") },
            onNavigateToArticles = { Log.d("Preview", "Navigate to Articles") },
            onNavigateToProfile = { Log.d("Preview", "Navigate to Profile") }
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 200)
@Composable
fun FeatureNavigationCardPreview() {
    HubRetroTheme {
        Box(modifier = Modifier.background(Color.Black).padding(16.dp)) { // Ensure background for visibility
            FeatureNavigationCard(
                title = "ALBUMS",
                description = "Groove to the classics. Soundtracks from legendary games await your ears.",
                imageResId = ALBUMS_CARD_IMAGE, // Use one of your defined image constants
                buttonText = "TAKE ME THERE",
                onButtonClick = {},
                gradientColors = listOf(VaporwavePink, VaporwaveBlue.copy(alpha = 0.7f))
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222222)
@Composable
fun CopyrightFooterPreview() {
    HubRetroTheme {
        CopyrightFooter(name = "Carlos Zabala", blogUrl = "https://charlysblog.framer.website")
    }
}
