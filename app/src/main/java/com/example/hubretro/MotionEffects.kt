package com.example.hubretro

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
// 1. HALFTONE IMAGE REVEAL
// Images appear as comic dots that grow until they merge into the full picture.
// Plays once per image (key), so scrolling back doesn't replay it.
// ═══════════════════════════════════════════════════════════════════════════════

private val revealedImages = HashSet<Any>()

@Composable
fun Modifier.halftoneReveal(key: Any?): Modifier {
    if (key == null) return this
    val first = remember(key) { revealedImages.add(key) }
    if (!first) return this
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    if (p.value >= 1f) return this
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val progress = p.value
            val spacing = 9.dp.toPx()
            // radius grows until neighbouring dots overlap and cover everything
            val r = progress * spacing * 0.75f
            val paint = Paint().apply { blendMode = BlendMode.DstIn }
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), paint)
                if (r > 0f) {
                    val cols = (size.width / spacing).toInt() + 2
                    val rows = (size.height / spacing).toInt() + 2
                    for (row in 0..rows) for (col in 0..cols) {
                        // offset every other row for a real halftone pattern
                        val x = col * spacing + if (row % 2 == 0) 0f else spacing / 2f
                        drawCircle(Color.Black, radius = r, center = Offset(x, row * spacing))
                    }
                }
                canvas.restore()
            }
        }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. DEALT-CARD ENTRANCE — slides up with a tilt and settles (used by SpringEntrance)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun Modifier.dealIn(delayMs: Int = 0, seed: Int = 0): Modifier {
    val p = remember { Animatable(0f) }
    val tilt = remember { if (seed % 2 == 0) -5f else 5f }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.coerceIn(0, 360).toLong())
        p.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 260f))
    }
    return this.graphicsLayer {
        val v = p.value
        translationY = (1f - v) * 42.dp.toPx()
        rotationZ = (1f - v) * tilt
        val s = 0.9f + 0.1f * v
        scaleX = s; scaleY = s
        alpha = v.coerceIn(0f, 1f)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. COMIC DIALOG POP — sheets/dialogs slam in like a comic panel
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun Modifier.comicPop(): Modifier {
    val scale = remember { Animatable(0.82f) }
    val rot = remember { Animatable(-4f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(140)) }
        launch { rot.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 420f)) }
        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 480f))
    }
    return this.graphicsLayer {
        scaleX = scale.value; scaleY = scale.value
        rotationZ = rot.value
        this.alpha = alpha.value
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. INK TAB ROW — thick ink underline that slides + stretches to the selected tab
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun InkTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var rowWidthPx by remember { mutableIntStateOf(0) }
    val pos by animateFloatAsState(selectedIndex.toFloat(),
        spring(dampingRatio = 0.62f, stiffness = 380f), label = "inkTabPos")
    val tap = rememberTapHaptic()

    Box(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp))
            .onGloballyPositioned { rowWidthPx = it.size.width }
    ) {
        val count = tabs.size.coerceAtLeast(1)
        val tabWidthPx = rowWidthPx / count.toFloat()
        // Ink underline: stretches while travelling between tabs
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 5.dp)
                .graphicsLayer {
                    val travel = kotlin.math.abs(pos - selectedIndex).coerceAtMost(1f)
                    translationX = pos * tabWidthPx + tabWidthPx * 0.2f
                    scaleX = 1f + travel * 0.6f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                }
                .width(with(density) { (tabWidthPx * 0.6f).toDp() })
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CGreen)
                .border(1.dp, ScrapbookDark, RoundedCornerShape(2.dp))
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier.weight(1f)
                        .squashOnSelect(selected)
                        .clickable { if (!selected) { tap(); onSelect(index) } }
                        .padding(top = 10.dp, bottom = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = TextStyle(
                            fontFamily = BangersFontFamily,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            color = if (selected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.45f)
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 5. JUMP-IN CHIPS — filter/console chips hop in one by one every time a page opens
// (drops from above, lands with a little squash). Replays on each page visit.
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun Modifier.jumpIn(index: Int): Modifier {
    val y = remember { Animatable(-26f) }
    val squash = remember { Animatable(1f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 55L).coerceAtMost(500L))
        launch { alpha.animateTo(1f, tween(120)) }
        y.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 520f))
        squash.snapTo(0.82f)
        squash.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 700f))
    }
    return this.graphicsLayer {
        translationY = y.value * density
        scaleY = squash.value
        scaleX = 2f - squash.value
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
        this.alpha = alpha.value
    }
}
