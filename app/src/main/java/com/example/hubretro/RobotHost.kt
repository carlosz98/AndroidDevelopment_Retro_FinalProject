package com.example.hubretro

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hubretro.ui.theme.*

@Composable
fun RobotHost(
    modifier: Modifier = Modifier,
    habboUsername: String = "",
    habboRegion: String = "habbo.com"
) {
    val message by RobotBrain.currentMessage.collectAsState()

    TalkingRobot(
        message = message?.text ?: "",
        isVisible = message != null,
        habboUsername = habboUsername,
        habboRegion = habboRegion,
        accentColor = message?.accentColor ?: CGreen,
        stance = message?.stance ?: RobotStance.STANDING,
        modifier = modifier.padding(start = 16.dp, bottom = 4.dp),
        onDismiss = { RobotBrain.dismiss() }
    )
}