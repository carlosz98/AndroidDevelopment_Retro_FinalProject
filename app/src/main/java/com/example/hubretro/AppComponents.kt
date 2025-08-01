// In AppComponents.kt
package com.example.hubretro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.AppBarBackground
import com.example.hubretro.ui.theme.AppBarTitleColor // Used for title, and default for contentColor
import com.example.hubretro.ui.theme.HubRetroTheme
import com.example.hubretro.ui.theme.RetroBorderColor
import com.example.hubretro.ui.theme.RetroFontFamily
import com.example.hubretro.ui.theme.VaporwavePink // <<< IMPORT VAPORWAVE PINK

// Assuming TopActionItem is defined (e.g., in MainActivity.kt or a shared file)
// data class TopActionItem(val label: String, val route: String)

private val previewActionItems = listOf(
    TopActionItem("HOME", "home"),
    TopActionItem("MAGAZINES", "magazines"),
    TopActionItem("ALBUMS", "albs"),
    TopActionItem("ARTICLES", "arts")
)

@Composable
fun RetroAppBar(
    title: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppBarBackground,
    contentColor: Color = AppBarTitleColor, // For title (light grey). Default if not overridden for MENU.
    borderColor: Color = RetroBorderColor,
    borderWidth: Dp = 2.dp,
    height: Dp = 56.dp,
    actionItems: List<TopActionItem> = emptyList(),
    selectedItemLabel: String? = null, // <<< NEW PARAMETER
    onActionItemClick: (TopActionItem) -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val defaultMenuButtonTextColor = Color(0xFFDCDCDC) // Your very light grey for "MENU" when closed

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor)
            .border(width = borderWidth, color = borderColor)
            .padding(horizontal = 8.dp), // Padding for AppBar content
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (title.isNotEmpty()) {
                Text(
                    text = title.uppercase(),
                    color = contentColor, // Uses AppBarTitleColor (light grey by default)
                    fontFamily = RetroFontFamily,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }

            Box { // Anchor for DropdownMenu
                MenuButton(
                    text = "MENU",
                    onClick = { menuExpanded = !menuExpanded }, // Toggle menu visibility
                    textColor = if (menuExpanded) VaporwavePink else defaultMenuButtonTextColor, // <<< MENU TEXT COLOR CHANGE
                    buttonBackgroundColor = Color(0xFF444455),
                    buttonBorderColor = Color(0xFF777788)
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .background(Color(0xFF333344))
                        .border(1.dp, RetroBorderColor)
                ) {
                    actionItems.forEach { item ->
                        val isSelected = item.label == selectedItemLabel
                        val textColor = if (isSelected) VaporwavePink else Color.White // Default White for non-selected

                        DropdownMenuItem(
                            text = {
                                Text(
                                    item.label.uppercase(),
                                    fontFamily = RetroFontFamily,
                                    fontSize = 14.sp,
                                    color = textColor // <<< APPLY CONDITIONAL COLOR
                                )
                            },
                            onClick = {
                                onActionItemClick(item)
                                menuExpanded = false // Close menu after item click
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// MenuButton composable remains the same
@Composable
fun MenuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonBackgroundColor: Color = Color(0xFF444455),
    textColor: Color = Color.White, // Default, but overridden by RetroAppBar
    buttonBorderColor: Color = Color(0xFF777788),
    borderWidth: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .background(buttonBackgroundColor)
            .border(width = borderWidth, color = buttonBorderColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .defaultMinSize(minHeight = 38.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            fontFamily = RetroFontFamily,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun RetroAppBarPreview_TextMenu_HomeSelected() {
    HubRetroTheme {
        RetroAppBar(
            title = "",
            actionItems = previewActionItems,
            selectedItemLabel = "HOME", // Preview with HOME selected
            onActionItemClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun RetroAppBarPreview_WithTitleAndTextMenu_MagazinesSelected() {
    HubRetroTheme {
        RetroAppBar(
            title = "EXPLORE",
            actionItems = previewActionItems,
            selectedItemLabel = "MAGAZINES", // Preview with MAGAZINES selected
            onActionItemClick = {}
        )
    }
}

// Preview to see the MENU button in pink (when dropdown would be open)
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun RetroAppBarPreview_MenuOpen() {
    var menuExpanded by remember { mutableStateOf(true) } // Simulate menu being open for preview
    val defaultMenuButtonTextColor = Color(0xFFDCDCDC)

    HubRetroTheme {
        RetroAppBar(
            title = "",
            actionItems = previewActionItems,
            onActionItemClick = {},
            // We can't directly control menuExpanded from here for the actual component in preview
            // but we can simulate the color change for the MenuButton if we were to expose a parameter
            // For simplicity, this preview doesn't perfectly show the internal state.
            // The MenuButton's textColor is determined by the internal 'menuExpanded' state.
            // To properly preview the "MENU" button pink, you'd click it in an interactive preview.
        )
        // If you wanted to directly preview MenuButton with pink:
        // MenuButton(text = "MENU", onClick = {}, textColor = VaporwavePink)
    }
}
