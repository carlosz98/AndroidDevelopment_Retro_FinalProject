package com.example.hubretro

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hubretro.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════════
// Comic + soft ("mix") style
//
//   Buttons POP OUT comic-style  → inkPop()     white pill, ink outline, hard ink shadow
//   Spaces and "on" states SINK  → softWell()   light mint, soft inner shadow (neumorphic)
//                                → softPressed() a well with a thin ink outline, for
//                                                selected chips / reactions you've tapped
//
// The outer cards keep the scrapbook look (white, thick ink border, hard offset shadow).
// ═══════════════════════════════════════════════════════════════════════════════

val SoftWellBg = Color(0xFFF1F7F3)          // mint-white inside of a well
val SoftWellDark = Color(0xFFD5E5DA)        // inner shadow, top-left
val SoftWellLight = Color(0xFFFFFFFF)       // inner highlight, bottom-right
val SoftPressedBg = Color(0xFFE3F2E9)       // a bit greener for "selected"

/** Soft inner shadow (top-left dark, bottom-right light), clipped to a rounded rect. */
private fun DrawScope.innerSoftShadow(radiusPx: Float, blurPx: Float, offsetPx: Float, dark: Color, light: Color) {
    val clip = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radiusPx, radiusPx))) }
    clipPath(clip) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = blurPx * 2f
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            val n = canvas.nativeCanvas
            // dark edge pushed in from the top-left
            paint.color = dark.toArgb()
            n.save(); n.translate(offsetPx, offsetPx)
            n.drawRoundRect(0f, 0f, size.width, size.height, radiusPx, radiusPx, paint)
            n.restore()
            // light edge pushed in from the bottom-right
            paint.color = light.toArgb()
            n.save(); n.translate(-offsetPx, -offsetPx)
            n.drawRoundRect(0f, 0f, size.width, size.height, radiusPx, radiusPx, paint)
            n.restore()
        }
    }
}

/** A soft well pressed into the card (photo frames, action tray, avatar ring). */
fun Modifier.softWell(
    radius: Dp = 14.dp,
    background: Color = SoftWellBg,
    inkBorder: Dp = 0.dp,
    depth: Dp = 3.dp
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(background)
        .drawBehind {
            innerSoftShadow(radius.toPx(), (depth * 1.6f).toPx(), (depth * 0.8f).toPx(), SoftWellDark, SoftWellLight)
        }
        .then(if (inkBorder > 0.dp) Modifier.border(inkBorder, ScrapbookDark, shape) else Modifier)
}

/** Something you've switched on: sunk in, light green, thin ink outline. */
fun Modifier.softPressed(radius: Dp = 12.dp): Modifier =
    softWell(radius = radius, background = SoftPressedBg, inkBorder = 1.5.dp, depth = 2.dp)

/** A comic button that pops out: fill, ink outline, hard ink shadow down-right. */
fun Modifier.inkPop(
    radius: Dp = 12.dp,
    fill: Color = Color.White,
    shadow: Dp = 2.dp,
    border: Dp = 1.5.dp
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .drawBehind {
            val s = shadow.toPx()
            drawRoundRect(
                color = ScrapbookDark,
                topLeft = Offset(s, s),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(radius.toPx(), radius.toPx())
            )
        }
        .clip(shape)
        .background(fill)
        .border(border, ScrapbookDark, shape)
}
