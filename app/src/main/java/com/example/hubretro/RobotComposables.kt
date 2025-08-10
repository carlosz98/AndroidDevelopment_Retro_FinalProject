package com.example.hubretro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
// import androidx.compose.ui.graphics.Color // Not needed if only using MaterialTheme colors
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TalkingRobot(
    message: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    robotSpriteResId: Int? = null // e.g., R.drawable.your_robot_sprite
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500)),
        exit = slideOutHorizontally(targetOffsetX = { it / 2 }, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Adds space between the robot and the bubble
        ) {
            // --- MODIFIED ORDER: Robot Character/Icon comes FIRST ---
            if (robotSpriteResId != null) {
                Image(
                    painter = painterResource(id = robotSpriteResId),
                    contentDescription = "Talking Robot", // Important for accessibility
                    modifier = Modifier.size(60.dp) // Adjust size as needed
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = "Talking Robot Placeholder", // Important for accessibility
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.secondary // Use a theme color for placeholder
                )
            }

            // --- Speech Bubble comes SECOND ---
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant, // Using theme color for bubble
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall, // Using typography from theme
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // Corresponding on-color
                    textAlign = TextAlign.Start // Text aligns to the start (left in LTR) of the bubble
                )
            }
        }
    }
}
