package com.example.hubretro

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.res.ResourcesCompat
import com.example.hubretro.ui.theme.CGreen
import com.example.hubretro.ui.theme.CGreenDeep
import com.example.hubretro.ui.theme.CGreenMint
import com.example.hubretro.ui.theme.ComicGlassBg
import com.example.hubretro.ui.theme.ScrapbookBlue
import com.example.hubretro.ui.theme.ScrapbookBorder
import com.example.hubretro.ui.theme.ScrapbookPurple
import com.example.hubretro.ui.theme.ScrapbookYellow
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════════
// RetroHub launch intro — all ASCII, drawn as a grid of monospace characters.
//
//   1. Campfire sting (~4.1s): on the app's light green background, a dark window
//      framed like a scrapbook card shows an isometric forest clearing lit only by
//      a campfire, then "b y / d o n c a r l i s" decodes under the fire.
//   2. Title intro (~4.35s), light theme: boot text, the RETRO HUB logo decodes,
//      an impact with shockwaves and sparks, a white box-drawn card with an ink
//      shadow and WELCOME, corner icons, PRESS START.
//
// Tap skips the current part (sting → title, title → app). Back skips everything.
// Skipped entirely when the device's animation scale is 0. Same public API as before,
// so MainActivity doesn't change.
// ═══════════════════════════════════════════════════════════════════════════════

// ── Settings: change these to customise the intro ─────────────────────────────
private const val STING_CREDIT_LINE = "by"           // small line above the name
private const val STING_NAME = "doncarlis"           // dev / studio name (shown spaced out)
private const val STING_WARM_FIRE = true             // false = green phosphor fire
private const val STING_EVERY_LAUNCH = true          // false = sting only on the very first launch
private const val INTRO_WELCOME_LINE = "[ WELCOME, PLAYER ]"

// ── Timing ────────────────────────────────────────────────────────────────────
private const val STING_DURATION_MS = 4100L
private const val TITLE_IMPACT_MS = 1600L
private const val TITLE_DURATION_MS = 4350L

// ── Grid metrics ──────────────────────────────────────────────────────────────
private const val ASCII_COLS = 46                    // characters across the screen
private const val ASCII_CELL_RATIO = 0.6f            // cell width / cell height (monospace)
private const val ASCII_RAMP = " .:-=+*#%@"
private const val ASCII_GLITCH = "!@#$%&*?01<>/=+"

private const val INTRO_PREFS = "retrohub_intro"
private const val KEY_STING_SEEN = "sting_seen"

// ── Palette (greens come from ui/theme/Color.kt) ──────────────────────────────
private val AC_MINT = ScrapbookYellow.toArgb()       // 0xFFB7E4C7 in this theme
private val AC_GREEN = CGreen.toArgb()               // 0xFF52B788
private val AC_LIGHT = CGreenMint.toArgb()           // 0xFF74C69D
private val AC_DEEP = CGreenDeep.toArgb()            // 0xFF40916C
private val AC_FOREST = ScrapbookBlue.toArgb()       // 0xFF2D6A4F
private val AC_DIM = ScrapbookPurple.toArgb()        // 0xFF1B4332
private val AC_INK = ScrapbookBorder.toArgb()        // 0xFF1A1A1A
private val AC_PAGE = ComicGlassBg.toArgb()          // 0xFFEFF7F2 — the app's light background
private val AC_WHITE = 0xFFFFFFFF.toInt()
private val AC_RAY = 0xFF95D5B2.toInt()
private val AC_NIGHT = 0xFF050F0B.toInt()            // inside the campfire window

private class FirePalette(
    val core: Int, val hot: Int, val mid: Int, val low: Int, val tip: Int,
    val ember: IntArray, val light: IntArray
)

private val WARM_FIRE = FirePalette(
    core = 0xFFFFF6D8.toInt(), hot = 0xFFFFD166.toInt(), mid = 0xFFFFB020.toInt(),
    low = 0xFFF3722C.toInt(), tip = 0xFF9C3A12.toInt(),
    ember = intArrayOf(0xFFFFD166.toInt(), 0xFFF3722C.toInt(), 0xFFFFF6D8.toInt()),
    light = intArrayOf(0xFF1E120A.toInt(), 0xFF3A1E0E.toInt(), 0xFF6B3413.toInt(), 0xFFB5581C.toInt(), 0xFFF0A04B.toInt())
)
private val GREEN_FIRE = FirePalette(
    core = AC_WHITE, hot = 0xFFD8F3DC.toInt(), mid = AC_MINT, low = AC_GREEN, tip = AC_FOREST,
    ember = intArrayOf(AC_MINT, AC_LIGHT, AC_WHITE),
    light = intArrayOf(0xFF0E2419.toInt(), AC_DIM, AC_FOREST, AC_GREEN, 0xFF95D5B2.toInt())
)
private val STING_FOLIAGE = intArrayOf(0xFF0B2116.toInt(), 0xFF14382A.toInt(), 0xFF1F5A3F.toInt(), 0xFF3F8F63.toInt(), 0xFF7CC59A.toInt())
private val STING_WATER = intArrayOf(0xFF071A1E.toInt(), 0xFF0E3438.toInt(), 0xFF16585A.toInt(), 0xFF2A8F86.toInt(), 0xFF7FD8CB.toInt())

// ═══════════════════════════════════════════════════════════════════════════════
// Public entry point
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun RetroHubIntro(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    onImpact: () -> Unit = {}
) {
    val context = LocalContext.current
    val finishCallback by rememberUpdatedState(onFinished)
    var done by remember { mutableStateOf(false) }
    val finish: () -> Unit = {
        if (!done) {
            done = true
            finishCallback()
        }
    }

    val playSting = remember {
        STING_EVERY_LAUNCH ||
            !context.getSharedPreferences(INTRO_PREFS, Context.MODE_PRIVATE).getBoolean(KEY_STING_SEEN, false)
    }
    // 0 = campfire sting, 1 = title intro
    var phase by remember { mutableIntStateOf(if (playSting) 0 else 1) }

    LaunchedEffect(Unit) {
        context.getSharedPreferences(INTRO_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_STING_SEEN, true).apply()
        val scale = Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        )
        if (scale == 0f) finish()
    }

    BackHandler(enabled = !done) { finish() }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Color(AC_PAGE))
            .pointerInput(Unit) {
                detectTapGestures { if (phase == 0) phase = 1 else finish() }
            }
    ) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        if (w > 0 && h > 0) {
            if (phase == 0) {
                CampfireSting(w, h, onDone = { phase = 1 })
            } else {
                AsciiTitleIntro(w, h, onImpact = onImpact, onDone = finish)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Shared ASCII plumbing
// ═══════════════════════════════════════════════════════════════════════════════

private class AsciiGrid(val cols: Int, val rows: Int, val cw: Float, val chH: Float, val density: Float) {
    val ch = CharArray(cols * rows) { ' ' }
    val col = IntArray(cols * rows)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textSize = chH * 0.98f
    }
    val fxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val clipPath = Path()
    val scanStep = 3f * density
    val scanH = 1f * density

    fun clear() = ch.fill(' ')

    fun put(x: Int, y: Int, c: Char, color: Int) {
        if (x < 0 || y < 0 || x >= cols || y >= rows) return
        val i = y * cols + x
        ch[i] = c
        col[i] = color
    }
}

@Composable
private fun rememberAsciiGrid(widthPx: Int, heightPx: Int): AsciiGrid {
    val density = LocalDensity.current.density
    return remember(widthPx, heightPx) {
        val cw = widthPx / ASCII_COLS.toFloat()
        val chH = cw / ASCII_CELL_RATIO
        AsciiGrid(ASCII_COLS, max(1, floor(heightPx / chH).toInt()), cw, chH, density)
    }
}

@Composable
private fun rememberSceneClock(): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> t.floatValue = (now - start) / 1_000_000_000f }
        }
    }
    return t
}

private fun drawAsciiCells(n: android.graphics.Canvas, g: AsciiGrid) {
    val baseline = g.chH * 0.8f
    for (y in 0 until g.rows) {
        val py = y * g.chH + baseline
        val rowStart = y * g.cols
        for (x in 0 until g.cols) {
            val i = rowStart + x
            if (g.ch[i] == ' ') continue
            g.textPaint.color = g.col[i]
            n.drawText(g.ch, i, 1, x * g.cw, py, g.textPaint)
        }
    }
}

private fun drawAsciiScanlines(n: android.graphics.Canvas, g: AsciiGrid, left: Float, top: Float, right: Float, bottom: Float, color: Int) {
    g.fxPaint.color = color
    var sy = top
    while (sy < bottom) {
        n.drawRect(left, sy, right, sy + g.scanH, g.fxPaint)
        sy += g.scanStep
    }
}

private fun drawFade(n: android.graphics.Canvas, g: AsciiGrid, w: Float, h: Float, color: Int, fade: Float) {
    if (fade <= 0f) return
    val a = (fade.coerceIn(0f, 1f) * 255f).toInt()
    g.fxPaint.color = (color and 0x00FFFFFF) or (a shl 24)
    n.drawRect(0f, 0f, w, h, g.fxPaint)
}

// ═══════════════════════════════════════════════════════════════════════════════
// Part 1 — Campfire sting
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CampfireSting(widthPx: Int, heightPx: Int, onDone: () -> Unit) {
    val grid = rememberAsciiGrid(widthPx, heightPx)
    val scene = remember(grid) { StingScene(grid) }
    val clock = rememberSceneClock()
    val doneCallback by rememberUpdatedState(onDone)

    LaunchedEffect(Unit) {
        delay(STING_DURATION_MS)
        doneCallback()
    }

    Canvas(Modifier.fillMaxSize()) {
        val t = clock.value
        scene.render(grid, t)
        drawIntoCanvas { canvas ->
            val n = canvas.nativeCanvas
            val r = scene.panel
            val radius = 12f * grid.density
            val shadow = 6f * grid.density
            n.drawColor(AC_PAGE)
            // Scrapbook hard shadow + dark window
            grid.fxPaint.color = AC_INK
            n.drawRoundRect(r.left + shadow, r.top + shadow, r.right + shadow, r.bottom + shadow, radius, radius, grid.fxPaint)
            grid.fxPaint.color = AC_NIGHT
            n.drawRoundRect(r.left, r.top, r.right, r.bottom, radius, radius, grid.fxPaint)
            // CRT scanlines inside the window only
            n.save()
            grid.clipPath.reset()
            grid.clipPath.addRoundRect(r, radius, radius, Path.Direction.CW)
            n.clipPath(grid.clipPath)
            drawAsciiScanlines(n, grid, r.left, r.top, r.right, r.bottom, 0x38000000)
            n.restore()
            // Characters (outside texture + the scene)
            drawAsciiCells(n, grid)
            // Ink border
            grid.strokePaint.color = AC_INK
            grid.strokePaint.strokeWidth = 3f * grid.density
            n.drawRoundRect(r.left, r.top, r.right, r.bottom, radius, radius, grid.strokePaint)
            // Fade into the light title intro
            drawFade(n, grid, size.width, size.height, AC_PAGE, ((t - 3.7f) / 0.4f).coerceIn(0f, 1f))
        }
    }
}

private const val T_GROUND: Byte = 0
private const val T_EDGE: Byte = 1
private const val T_WATER: Byte = 2
private const val T_DIRT: Byte = 3
private const val T_CLIFF: Byte = 4
private const val T_FOL: Byte = 5
private const val T_WOOD: Byte = 6
private const val T_ROCK: Byte = 7

private val STING_OAK = arrayOf(
    "  .@@@@.  ",
    " @@@%@@@@ ",
    "@@%@@@%@@@",
    " @@@@%@@@ ",
    "  @@%@@@  ",
    "   )||(   ",
    "    ||    "
)
private val STING_PINE = arrayOf(
    "    ^    ",
    "   /^\\   ",
    "  //|\\\\  ",
    " ///|\\\\\\ ",
    "////|\\\\\\\\",
    "    |    "
)
private val STING_SMALL_PINE = arrayOf(
    "  ^  ",
    " /|\\ ",
    "//|\\\\",
    "  |  "
)
private val STING_BENCH = arrayOf(
    "  _______ ",
    " /______/|",
    " ||    || "
)
private val STING_BUSH = arrayOf(
    " .@%. ",
    "(@%@%)"
)
private val STING_ROCKS = arrayOf(
    "   _  ",
    " _(O)o",
    "(o)   "
)
private val STING_LOGS = arrayOf(
    "\\\\  //",
    "=\\\\//=",
    "//  \\\\"
)

private class StingLayer(n: Int) {
    val ch = CharArray(n) { ' ' }
    val type = ByteArray(n)
    val anchor = IntArray(n)
    val height = IntArray(n)
}

private class StingStar(val x: Int, val y: Int, val phase: Float)

private class StingEmber(
    val x: Float, val y: Float, val vx: Float, val vy: Float, val born: Float, val color: Int
)

private class StingScene(grid: AsciiGrid) {
    private val cols = grid.cols
    private val rows = grid.rows
    private val fx = cols / 2
    private val cyD = (rows * 0.42f).toInt()
    private val fireRow = cyD + 1
    private val a = max(8, (cols * 0.353f).roundToInt())    // diamond half-width (cells)
    private val b = max(4, (a / 2.9f).roundToInt())          // diamond half-height (rows)
    private val sc = a / 23f                                 // scale for layout offsets

    // The dark window (in cells, then pixels)
    private val px0 = 3
    private val px1 = cols - 4
    private val py0 = max(1, cyD - 12)
    private val py1 = min(rows - 3, cyD + b + 11)
    val panel = RectF(
        px0 * grid.cw - 2f * grid.density,
        py0 * grid.chH - 3f * grid.density,
        (px1 + 1) * grid.cw + 2f * grid.density,
        (py1 + 1) * grid.chH + 3f * grid.density
    )

    private val back = StingLayer(cols * rows)
    private val front = StingLayer(cols * rows)
    private val stars: List<StingStar>
    private val embers = ArrayList<StingEmber>()
    private val nameSeeds = FloatArray(30) { Random.nextFloat() * 0.5f }

    private fun o(n: Int) = (n * sc).roundToInt()

    private fun inPanel(x: Int, y: Int) = x in px0..px1 && y in py0..py1

    // Cells covered by the panel's hard shadow (keep the outside texture off them)
    private fun inShadow(x: Int, y: Int) =
        (x in (px1 + 1)..(px1 + 2) && y in (py0 + 1)..(py1 + 1)) || (y == py1 + 1 && x in (px0 + 1)..(px1 + 2))

    private fun set(layer: StingLayer, x: Int, y: Int, c: Char, type: Byte, anchor: Int, height: Int) {
        if (x < 0 || y < 0 || x >= cols || y >= rows) return
        val i = y * cols + x
        layer.ch[i] = c
        layer.type[i] = type
        layer.anchor[i] = anchor
        layer.height[i] = height
    }

    private fun stamp(layer: StingLayer, art: Array<String>, cx: Int, bottom: Int, type: Byte, isTree: Boolean) {
        val h = art.size
        val w = art[0].length
        for (ly in 0 until h) for (lx in 0 until w) {
            val c = art[ly][lx]
            if (c == ' ') continue
            val t = if (isTree) (if (c == '|' || c == ')' || c == '(') T_WOOD else T_FOL) else type
            set(layer, cx - w / 2 + lx, bottom - h + 1 + ly, c, t, bottom, h - 1 - ly)
        }
    }

    init {
        val groundCh = ",.'`\","
        val dirtCh = ":.:'."
        val cliffCh = "|:#|:"
        for (y in 0 until rows) for (x in 0 until cols) {
            val dx = x - fx
            val dy = y - cyD
            val v = abs(dx).toFloat() / a + abs(dy).toFloat() / b
            if (v > 1f) continue
            val pdx = (x - (fx + o(11))).toFloat()
            val pdy = (y - (cyD + 3)) * 2.6f
            val pond = sqrt(pdx * pdx + pdy * pdy) < 6f * sc
            val rdy = (y - fireRow) * 2.9f
            val rd = sqrt(dx.toFloat() * dx + rdy * rdy)
            when {
                v > 0.9f -> {
                    val c = if (dy < 0) (if (Random.nextBoolean()) '.' else '\'') else (if (dx < 0) '\\' else '/')
                    set(back, x, y, c, T_EDGE, y, 0)
                }
                pond -> set(back, x, y, '~', T_WATER, y, 0)
                rd < 7f && rd > 2.5f -> set(back, x, y, dirtCh[Random.nextInt(dirtCh.length)], T_DIRT, y, 0)
                else -> {
                    val r = Random.nextFloat()
                    when {
                        r < 0.45f -> set(back, x, y, groundCh[Random.nextInt(groundCh.length)], T_GROUND, y, 0)
                        r < 0.5f -> set(back, x, y, if (Random.nextBoolean()) 'v' else 'w', T_GROUND, y, 0)
                        r < 0.52f -> set(back, x, y, 'o', T_GROUND, y, 0)
                    }
                }
            }
        }
        // Cliff under the floating tile
        for (x in 0 until cols) {
            val dx2 = abs(x - fx)
            if (dx2 > a) continue
            val ybot = (cyD + b * (1f - dx2.toFloat() / a)).roundToInt()
            for (k in 1..3) {
                val c = if (k == 3) (if (x < fx) '\\' else '/') else cliffCh[(x + k) % 5]
                set(back, x, ybot + k, c, T_CLIFF, ybot, 0)
            }
        }
        // Trees behind the fire
        stamp(back, STING_SMALL_PINE, fx - o(17), cyD - 3, T_FOL, true)
        stamp(back, STING_SMALL_PINE, fx + o(16), cyD - 3, T_FOL, true)
        stamp(back, STING_PINE, fx - o(10), cyD - 3, T_FOL, true)
        stamp(back, STING_OAK, fx + o(8), cyD - 2, T_FOL, true)
        stamp(back, STING_OAK, fx - o(20), cyD + 1, T_FOL, true)
        stamp(back, STING_PINE, fx + o(19), cyD + 1, T_FOL, true)
        // Things in front of the fire
        stamp(front, STING_BENCH, fx - o(11), cyD + 3, T_WOOD, false)
        stamp(front, STING_BUSH, fx - o(17), cyD + 3, T_FOL, false)
        stamp(front, STING_ROCKS, fx + o(4), cyD + 5, T_ROCK, false)
        stamp(front, STING_BUSH, fx + o(17), cyD + 4, T_FOL, false)
        stamp(front, STING_SMALL_PINE, fx - o(4), cyD + 6, T_FOL, true)

        val starRows = max(2, cyD - 9 - py0)
        stars = List(16) {
            StingStar(px0 + 1 + Random.nextInt(max(1, px1 - px0 - 1)), py0 + 1 + Random.nextInt(starRows), Random.nextFloat() * 6f)
        }
    }

    private fun drawLayer(g: AsciiGrid, layer: StingLayer, radius: Float, p: FirePalette, t: Float) {
        if (radius <= 0f) return
        val iso = a.toFloat() / b
        val waterShift = (t * 4f).toInt()
        for (y in 0 until rows) for (x in 0 until cols) {
            val i = y * cols + x
            val c = layer.ch[i]
            if (c == ' ' || !inPanel(x, y)) continue
            val dx = (x - fx).toFloat()
            val dyw = (layer.anchor[i] - fireRow) * iso
            val d = sqrt(dx * dx + dyw * dyw)
            var l = max(0f, 1f - d / radius) * (1f - layer.height[i] * 0.08f)
            when (layer.type[i]) {
                T_CLIFF -> l *= 0.6f
                T_DIRT -> l = min(1f, l * 1.2f)
            }
            if (l < 0.05f) continue
            val k = min(4, (l * 5.5f).toInt())
            when (layer.type[i]) {
                T_FOL -> g.put(x, y, c, STING_FOLIAGE[k])
                T_WATER -> g.put(x, y, if ((x + waterShift) % 3 == 0) '-' else '~', STING_WATER[k])
                else -> g.put(x, y, c, p.light[k])
            }
        }
    }

    fun render(g: AsciiGrid, t: Float) {
        g.clear()
        val p = if (STING_WARM_FIRE) WARM_FIRE else GREEN_FIRE
        var grow = ((t - 0.2f) / 0.7f).coerceIn(0f, 1f)
        grow = 1f - (1f - grow).pow(3)

        // Light-page texture outside the window
        for (y in 0 until rows) for (x in 0 until cols) {
            if (inPanel(x, y) || inShadow(x, y)) continue
            val v = (sin(x * 0.21f + t * 2.2f) + sin(y * 0.33f - t * 1.7f) + sin((x + y) * 0.11f + t * 0.9f)) / 3f
            if (v > 0.2f) g.put(x, y, ASCII_RAMP[1 + min(4, ((v - 0.2f) * 5f).toInt())], AC_MINT)
        }

        // Stars
        for (s in stars) {
            val v = (sin(t * 2f + s.phase) + 1f) / 2f
            if (v > 0.35f) g.put(s.x, s.y, if (v > 0.85f) '*' else '.', AC_DIM)
        }

        // Light radius flickers with the flames
        val radius = 30f * sc * grow *
            (0.9f + 0.06f * sin(t * 11f) + 0.04f * sin(t * 23.7f) + Random.nextFloat() * 0.04f)
        drawLayer(g, back, radius, p, t)

        // Logs
        STING_LOGS.forEachIndexed { ly, line ->
            val off = fx - line.length / 2
            line.forEachIndexed { lx, c ->
                if (c != ' ') {
                    val color = if (grow > 0.1f) p.light[if (ly == 1) 4 else 3] else p.light[0]
                    g.put(off + lx, fireRow - 1 + ly, c, color)
                }
            }
        }

        // Fire
        val fh = 6f
        val fw = 4
        for (ddx in -fw..fw) {
            val edge = 1f - abs(ddx) / (fw + 1f)
            val hcol = fh * grow * edge.pow(0.9f) *
                (0.72f + 0.18f * sin(t * 9f + ddx * 1.9f) + 0.12f * sin(t * 15.3f + ddx * 0.7f) + Random.nextFloat() * 0.12f)
            var h = 0
            while (h < hcol) {
                val rel = h / max(1f, hcol)
                val core = abs(ddx) < 2
                val (c, color) = when {
                    rel < 0.3f -> "@#%"[Random.nextInt(3)] to (if (core) p.core else p.hot)
                    rel < 0.6f -> "*#(){}"[Random.nextInt(6)] to (if (core) p.hot else p.mid)
                    rel < 0.85f -> "^()*"[Random.nextInt(4)] to p.low
                    else -> "'`.^"[Random.nextInt(4)] to p.tip
                }
                g.put(fx + ddx, fireRow - 2 - h, c, color)
                h++
            }
        }

        // Embers
        if (grow > 0.5f && Random.nextFloat() < 0.45f) {
            embers.add(
                StingEmber(
                    x = fx + (Random.nextFloat() - 0.5f) * 5f,
                    y = (fireRow - 5).toFloat(),
                    vx = (Random.nextFloat() - 0.5f) * 4f,
                    vy = -(5f + Random.nextFloat() * 7f),
                    born = t,
                    color = p.ember[Random.nextInt(p.ember.size)]
                )
            )
        }
        embers.removeAll { t - it.born >= 1.8f }
        for (e in embers) {
            val age = t - e.born
            val ex = (e.x + e.vx * age + sin(age * 6f + e.vx) * 1.2f).roundToInt()
            val ey = (e.y + e.vy * age).roundToInt()
            if (!inPanel(ex, ey)) continue
            val c = if (age < 0.5f) '*' else if (age < 1.2f) '\'' else '.'
            g.put(ex, ey, c, if (age < 1.2f) e.color else p.tip)
        }

        drawLayer(g, front, radius, p, t)

        // Credit: "b y" then "d o n c a r l i s"
        val ny = cyD + b + 5
        val by = STING_CREDIT_LINE.toCharArray().joinToString(" ")
        val bn = floor((t - 0.95f) * 30f).toInt()
        val bx = fx - by.length / 2
        for (i in 0 until min(by.length, max(0, bn))) {
            if (by[i] == ' ') continue
            g.put(bx + i, ny, by[i], if (i >= bn - 1 && bn < by.length) AC_WHITE else AC_GREEN)
        }
        val m = STING_NAME.toCharArray().joinToString(" ")
        val mx = fx - m.length / 2
        if (t >= 1.1f) {
            for (i in m.indices) {
                if (m[i] == ' ') continue
                val settle = 1.35f + i * 0.04f + nameSeeds[i % nameSeeds.size] * 0.4f
                if (t < settle) {
                    g.put(mx + i, ny + 2, ASCII_GLITCH[Random.nextInt(ASCII_GLITCH.length)], AC_DEEP)
                } else {
                    val shine = t > 2.15f && abs(i - (t - 2.15f) * 40f) < 2f
                    g.put(mx + i, ny + 2, m[i], if (shine) AC_WHITE else AC_MINT)
                }
            }
        }
        val ul = (((t - 1.85f) / 0.35f).coerceIn(0f, 1f) * m.length).toInt()
        for (i in 0 until ul) g.put(mx + i, ny + 3, '─', AC_FOREST)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Part 2 — ASCII title intro (light theme)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AsciiTitleIntro(widthPx: Int, heightPx: Int, onImpact: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val grid = rememberAsciiGrid(widthPx, heightPx)
    val scene = remember(grid) {
        val bangers = runCatching { ResourcesCompat.getFont(context, R.font.bangers_regular) }.getOrNull()
        TitleScene(grid.cols, grid.rows, bangers)
    }
    val clock = rememberSceneClock()
    val impactCallback by rememberUpdatedState(onImpact)
    val doneCallback by rememberUpdatedState(onDone)

    LaunchedEffect(Unit) {
        delay(TITLE_IMPACT_MS)
        impactCallback()
        delay(TITLE_DURATION_MS - TITLE_IMPACT_MS)
        doneCallback()
    }

    Canvas(Modifier.fillMaxSize()) {
        val t = clock.value
        val boxP = scene.render(grid, t)
        val shake = if (t > 1.6f && t < 1.95f) (1f - (t - 1.6f) / 0.35f) * 4f * grid.density else 0f
        drawIntoCanvas { canvas ->
            val n = canvas.nativeCanvas
            n.drawColor(AC_PAGE)
            n.save()
            n.translate((Random.nextFloat() - 0.5f) * 2f * shake, (Random.nextFloat() - 0.5f) * 2f * shake)
            // White card behind the logo once the frame is drawn
            if (boxP >= 1f) {
                grid.fxPaint.color = AC_WHITE
                n.drawRect(
                    scene.bx0 * grid.cw + grid.cw / 2f, scene.by0 * grid.chH + grid.chH / 2f,
                    scene.bx1 * grid.cw + grid.cw / 2f, scene.by1 * grid.chH + grid.chH / 2f,
                    grid.fxPaint
                )
            }
            drawAsciiCells(n, grid)
            n.restore()
            drawAsciiScanlines(n, grid, 0f, 0f, size.width, size.height, 0x09000000)
            val bandY = (t * 150f * grid.density) % size.height
            grid.fxPaint.color = (AC_GREEN and 0x00FFFFFF) or (0x0F shl 24)
            n.drawRect(0f, bandY, size.width, bandY + grid.chH, grid.fxPaint)
            drawFade(n, grid, size.width, size.height, AC_PAGE, ((t - 4.0f) / 0.35f).coerceIn(0f, 1f))
        }
    }
}

private val TITLE_BOOT_LINES = listOf(
    "RETROHUB BIOS v2.6",
    "CHECKING MEMORY......... OK",
    "LOADING CARTRIDGES...... OK",
    "> RUN WELCOME.EXE"
)

private class AsciiIcon(val art: Array<String>, val left: Boolean, val top: Boolean, val t0: Float)

private val TITLE_ICONS = listOf(
    AsciiIcon(arrayOf(" _=====_ ", "/ _   _ \\", "|(_) (_)|"), left = true, top = true, t0 = 2.9f),
    AsciiIcon(arrayOf("  .-.  ", " ( o ) ", "  `-'  "), left = false, top = true, t0 = 3.0f),
    AsciiIcon(arrayOf(" \\ | / ", "-- * --", " / | \\ "), left = true, top = false, t0 = 3.1f),
    AsciiIcon(arrayOf("  .-.  ", " |   | ", " d   b "), left = false, top = false, t0 = 3.2f)
)

private class TitleSpark(val angle: Float, val speed: Float, val c: Char, val color: Int)

private class TitleScene(val cols: Int, val rows: Int, bangers: Typeface?) {
    private val logo = FloatArray(cols * rows)
    private val seeds = FloatArray(cols * rows)
    val bx0: Int
    val bx1: Int
    val by0: Int
    val by1: Int
    private val ctrX: Float
    private val ctrY: Float
    private val perim: IntArray
    private val sparks: List<TitleSpark>

    init {
        // Rasterise "RETRO / HUB" in Bangers at grid resolution; each pixel = one cell.
        val bmp = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bangers ?: Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.BLACK
            textSize = cols / 3f
        }
        val fm = paint.fontMetrics
        val mid = -(fm.ascent + fm.descent) / 2f
        c.save()
        c.translate(cols / 2f, rows * 0.44f)
        c.scale(1f, ASCII_CELL_RATIO)
        c.drawText("RETRO", 0f, -paint.textSize * 0.47f + mid, paint)
        c.drawText("HUB", 0f, paint.textSize * 0.5f + mid, paint)
        c.restore()
        val px = IntArray(cols * rows)
        bmp.getPixels(px, 0, cols, 0, 0, cols, rows)
        bmp.recycle()

        var mnx = cols
        var mxx = 0
        var mny = rows
        var mxy = 0
        for (y in 0 until rows) for (x in 0 until cols) {
            val i = y * cols + x
            val l = (px[i] ushr 24) / 255f
            logo[i] = l
            seeds[i] = 0.85f + Random.nextFloat() * 0.6f + x.toFloat() / cols * 0.25f
            if (l > 0.12f) {
                mnx = min(mnx, x); mxx = max(mxx, x); mny = min(mny, y); mxy = max(mxy, y)
            }
        }
        if (mxx < mnx) { // font failed to draw — fall back to a sensible box
            mnx = cols / 4; mxx = cols * 3 / 4; mny = rows / 3; mxy = rows / 2
        }
        bx0 = max(1, mnx - 3)
        bx1 = min(cols - 3, mxx + 3)
        by0 = max(5, mny - 2)
        by1 = min(rows - 7, mxy + 4)
        ctrX = (bx0 + bx1) / 2f
        ctrY = (mny + mxy) / 2f

        val pl = ArrayList<Int>()
        for (x in bx0..bx1) { pl.add(x); pl.add(by0) }
        for (y in by0 + 1..by1) { pl.add(bx1); pl.add(y) }
        for (x in bx1 - 1 downTo bx0) { pl.add(x); pl.add(by1) }
        for (y in by1 - 1 downTo by0 + 1) { pl.add(bx0); pl.add(y) }
        perim = pl.toIntArray()

        val sparkChars = charArrayOf('*', '+', 'x', '.', 'o')
        val sparkColors = intArrayOf(AC_INK, AC_GREEN, AC_DEEP, AC_LIGHT)
        sparks = List(46) { i ->
            TitleSpark(
                angle = Random.nextFloat() * 2f * PI.toFloat(),
                speed = 14f + Random.nextFloat() * 26f,
                c = sparkChars[i % sparkChars.size],
                color = sparkColors[i % sparkColors.size]
            )
        }
    }

    private fun boxChar(x: Int, y: Int): Char = when {
        x == bx0 && y == by0 -> '┌'
        x == bx1 && y == by0 -> '┐'
        x == bx0 && y == by1 -> '└'
        x == bx1 && y == by1 -> '┘'
        y == by0 || y == by1 -> '─'
        else -> '│'
    }

    /** Fills the grid for time [t]; returns how far the card frame has drawn (0..1). */
    fun render(g: AsciiGrid, t: Float): Float {
        g.clear()
        val inv = 1f / ASCII_CELL_RATIO
        val boxP = ((t - 1.9f) / 0.5f).coerceIn(0f, 1f)
        val rot = t * 0.06f
        val rayOn = ((t - 1.75f) / 0.4f).coerceIn(0f, 1f)
        val twoPi = 2f * PI.toFloat()

        // Background: rotating rays after impact + slow plasma
        for (y in 0 until rows) for (x in 0 until cols) {
            if (boxP > 0f && x > bx0 && x < bx1 && y > by0 && y < by1) continue
            val dx = x - ctrX
            val dy = (y - ctrY) * inv
            val d = sqrt(dx * dx + dy * dy)
            if (rayOn > 0f && d > 12f) {
                var ang = (atan2(dy, dx) / twoPi * 14f + rot) % 1f
                if (ang < 0f) ang += 1f
                if (ang < 0.22f * rayOn) {
                    g.put(x, y, if (d > 30f) '.' else ':', AC_RAY)
                    continue
                }
            }
            val v = (sin(x * 0.21f + t * 2.2f) + sin(y * 0.33f - t * 1.7f) + sin((x + y) * 0.11f + t * 0.9f)) / 3f
            if (v > 0.2f) g.put(x, y, ASCII_RAMP[1 + min(4, ((v - 0.2f) * 5f).toInt())], AC_MINT)
        }

        // Boot text
        if (t < 2.0f) {
            TITLE_BOOT_LINES.forEachIndexed { r, s ->
                val shown = floor((t - r * 0.17f) * 70f).toInt()
                for (i in 0 until min(s.length, max(0, shown))) {
                    g.put(1 + i, 1 + r, s[i], if (i >= shown - 1) AC_INK else AC_FOREST)
                }
            }
        }

        // Logo: glitch → decode → impact flash → periodic shine
        val sh = t - 2.35f
        val wx = if (sh > 0f) (sh % 2.6f) * cols * 1.4f - 6f else 9999f
        val flash = t > 1.6f && t < 1.75f
        for (y in 0 until rows) for (x in 0 until cols) {
            val i = y * cols + x
            val l = logo[i]
            if (l <= 0.12f) continue
            if (t < seeds[i]) {
                if (t > 0.7f) g.put(x, y, ASCII_GLITCH[Random.nextInt(ASCII_GLITCH.length)], AC_LIGHT)
                continue
            }
            if (flash) {
                g.put(x, y, '@', AC_INK)
                continue
            }
            val shine = abs(x - wx - (y - ctrY) * 0.6f) < 1.6f
            g.put(x, y, ASCII_RAMP[min(9, (l * 9f).toInt() + 1)], if (shine) AC_INK else if (l > 0.6f) AC_FOREST else AC_GREEN)
        }

        // Shockwave rings
        for ((ringDelay, ringColor) in listOf(0f to AC_INK, 0.12f to AC_GREEN)) {
            val dt = t - 1.6f - ringDelay
            if (dt < 0f || dt > 0.8f) continue
            val rr = dt * 48f
            val c = if (dt < 0.3f) '#' else if (dt < 0.55f) '*' else '.'
            val color = if (dt < 0.4f) ringColor else AC_MINT
            for (y in 0 until rows) for (x in 0 until cols) {
                val dx = x - ctrX
                val dy = (y - ctrY) * inv
                if (abs(sqrt(dx * dx + dy * dy) - rr) < 0.9f) g.put(x, y, c, color)
            }
        }

        // Sparks
        val sdt = t - 1.6f
        if (sdt > 0f && sdt < 1.4f) {
            for (s in sparks) {
                val dist = s.speed * (1f - exp(-2.6f * sdt)) / 2.6f * 3.2f
                val sx = (ctrX + cos(s.angle) * dist).roundToInt()
                val sy = (ctrY + sin(s.angle) * dist * ASCII_CELL_RATIO).roundToInt()
                g.put(sx, sy, if (sdt > 1f) '.' else s.c, if (sdt > 1f) AC_MINT else s.color)
            }
        }

        // Card frame + solid ink shadow (scrapbook style)
        if (boxP > 0f) {
            val n = (perim.size / 2 * boxP).toInt()
            for (k in 0 until n) {
                val x = perim[k * 2]
                val y = perim[k * 2 + 1]
                g.put(x, y, boxChar(x, y), AC_INK)
            }
            if (boxP >= 1f) {
                for (y in by0 + 1..by1 + 1) g.put(bx1 + 1, y, '█', AC_INK)
                for (x in bx0 + 1..bx1 + 1) g.put(x, by1 + 1, '█', AC_INK)
            }
        }

        // Welcome line
        val tn = floor((t - 2.45f) * 40f).toInt()
        if (tn > 0) {
            val tag = INTRO_WELCOME_LINE
            val tx = (ctrX - tag.length / 2f).roundToInt()
            val ty = by1 - 2
            for (i in 0 until min(tag.length, tn)) {
                g.put(tx + i, ty, tag[i], if (i >= tn - 1 && tn < tag.length) AC_GREEN else AC_INK)
            }
        }

        // Corner icons
        TITLE_ICONS.forEachIndexed { k, ic ->
            val dt = t - ic.t0
            if (dt < 0f) return@forEachIndexed
            val w = ic.art[0].length
            val px = if (ic.left) 2 else cols - w - 2
            val py = if (ic.top) 1 else rows - 6
            val bob = if (t > 3.6f) ((t * 2f + k).toInt() % 2) else 0
            val color = if (dt < 0.12f) AC_INK else if (k % 2 == 1) AC_GREEN else AC_DEEP
            ic.art.forEachIndexed { ly, line ->
                line.forEachIndexed { lx, c -> if (c != ' ') g.put(px + lx, py + ly - bob, c, color) }
            }
        }

        // Press start
        if (t > 3.35f) {
            val txt = "> PRESS START"
            val on = (t * 2f).toInt() % 2 == 0
            val px0 = (cols / 2f - (txt.length + 1) / 2f).roundToInt()
            val py0 = rows - 2
            txt.forEachIndexed { i, c -> if (c != ' ') g.put(px0 + i, py0, c, if (on) AC_INK else AC_DEEP) }
            if (on) g.put(px0 + txt.length + 1, py0, '█', AC_INK)
        }

        return boxP
    }
}
