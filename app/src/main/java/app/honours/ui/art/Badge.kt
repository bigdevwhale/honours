package app.honours.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import app.honours.domain.Look
import app.honours.domain.Metal
import app.honours.domain.Shape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The colours of a metal: its body, the light catching it, the shade, and the recess of a relief. */
data class Tone(val base: Color, val hi: Color, val lo: Color, val deep: Color)

/**
 * An enamel pin badge, drawn in a unit space (radius 1) scaled to its place: a metal rim in the shape of
 * the badge, hot enamel inside it in two colours, a stamped relief, and a gloss that follows the light.
 */
object Badges {
    val ENAMEL = listOf(
        Color(0xFFC8302B), Color(0xFF2E8A55), Color(0xFF3B8ED3), Color(0xFF25407C),
        Color(0xFF2A282C), Color(0xFFF1EADB), Color(0xFFE3A82B), Color(0xFF7C1D2C),
    )

    fun tone(m: Metal) = when (m) {
        Metal.BRONZE -> Tone(Color(0xFFB8743A), Color(0xFFF2B87E), Color(0xFF6E3F18), Color(0xFF4A2A10))
        Metal.SILVER -> Tone(Color(0xFFBCC1C8), Color(0xFFF8F9FB), Color(0xFF6E747E), Color(0xFF4A4E56))
        Metal.GOLD -> Tone(Color(0xFFD8AB3A), Color(0xFFFFF0A8), Color(0xFF8C6418), Color(0xFF5E420C))
    }

    /** Pixel drawings use digit 1 for the metal and 2–7 for these enamels. */
    private val PIXEL = listOf(0, 5, 2, 1, 6, 4)
    fun pixelColor(d: Int, t: Tone): Color? = when (d) { 0 -> null; 1 -> t.base; else -> ENAMEL[PIXEL[(d - 2).coerceIn(0, 5)]] }
    const val PIXEL_COLORS = 7

    private fun svg(d: String) = PathParser().parsePathString(d).toPath()

    private fun star(points: Int, outer: Float, inner: Float) = Path().apply {
        for (i in 0 until points * 2) {
            val a = -PI / 2 + i * PI / points
            val r = if (i % 2 == 0) outer else inner
            val x = (cos(a) * r).toFloat(); val y = (sin(a) * r).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun gear(): Path = Path().apply {
        val n = 12
        val period = 2 * PI / n
        // Each tooth: up from the root, across the top, down again, then along the root to the next.
        val steps = listOf(0.0 to .84, .14 to 1.0, .5 to 1.0, .64 to .84)
        for (i in 0 until n) for ((j, st) in steps.withIndex()) {
            val a = -PI / 2 + (i + st.first) * period
            val x = (cos(a) * st.second).toFloat(); val y = (sin(a) * st.second).toFloat()
            if (i == 0 && j == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private val outlines = HashMap<Shape, Path>()

    /** The outer edge of a shape in unit space. */
    fun outline(s: Shape): Path = outlines.getOrPut(s) {
        when (s) {
            Shape.ROUND, Shape.MEDAL, Shape.WREATH, Shape.ORDER -> Path().apply { addOval(Rect(Offset.Zero, 1f)) }
            Shape.STAR -> star(5, 1.04f, .52f)
            Shape.SHIELD -> svg("M-0.84 -0.92 Q0 -0.78 0.84 -0.92 V0.02 C0.84 0.56 0.42 0.84 0 1 C-0.42 0.84 -0.84 0.56 -0.84 0.02 Z")
            Shape.FLAG -> svg("M-0.96 -0.72 C-0.55 -0.98 -0.18 -0.5 0.22 -0.7 C0.55 -0.86 0.8 -0.8 0.96 -0.72 V0.6 C0.62 0.46 0.34 0.82 0 0.66 C-0.38 0.5 -0.68 0.66 -0.96 0.76 Z")
            Shape.RHOMB -> Path().apply { moveTo(0f, -1.04f); lineTo(.8f, 0f); lineTo(0f, 1.04f); lineTo(-.8f, 0f); close() }
            Shape.GEAR -> gear()
        }
    }

    private fun scaled(p: Path, k: Float) = Path().apply { addPath(p); transform(Matrix().apply { scale(k, k) }) }

    private fun metalBrush(t: Tone, light: Offset) = Brush.linearGradient(
        0f to t.hi, .45f to t.base, 1f to t.lo,
        start = Offset(light.x * .9f, light.y * .9f), end = Offset(-light.x * .9f, -light.y * .9f),
    )

    /**
     * Draws [look] centred at [c] with radius [r] in pixels. [light] is where the light comes from, in unit
     * space (about −1…1 each way); the gloss and the metal follow it as the phone tilts.
     */
    fun DrawScope.badge(look: Look, metal: Metal, c: Offset, r: Float, light: Offset = Offset(-.5f, -.7f), shadow: Boolean = true, tilt: Float = 0f) {
        val t = tone(metal)
        withTransform({ translate(c.x, c.y); rotate(tilt, Offset.Zero); scale(r, r, Offset.Zero) }) {
            when (look.shape) {
                Shape.MEDAL -> medal(look, t, light, shadow)
                Shape.WREATH -> wreath(look, t, light, shadow)
                Shape.ORDER -> order(look, t, light, shadow)
                else -> plain(look, t, light, shadow)
            }
        }
    }

    /** A not-yet-earned badge: its shape pressed into the paper, no metal, no colour. */
    fun DrawScope.blank(shape: Shape, c: Offset, r: Float, fill: Color, edge: Color) {
        withTransform({ translate(c.x, c.y); scale(r, r, Offset.Zero) }) {
            val o = outline(shape)
            drawPath(o, fill)
            drawPath(o, edge, style = Stroke(.06f, join = StrokeJoin.Round))
            drawPath(scaled(o, .78f), edge.copy(alpha = edge.alpha * .5f), style = Stroke(.03f, join = StrokeJoin.Round))
        }
    }

    private fun DrawScope.drop(p: Path) = translate(.05f, .09f) { drawPath(p, Color(0x55000000)) }

    private fun DrawScope.gloss(clip: Path, light: Offset) = clipPath(clip) {
        drawCircle(Brush.radialGradient(listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF), Color(0x00FFFFFF)), center = light * .7f, radius = 1.1f), 1.6f, Offset.Zero)
        // A narrow streak of light across the enamel, as on a real glossy pin.
        rotate(-35f, Offset.Zero) {
            drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x40FFFFFF), Color(0x00FFFFFF)), startX = light.x * .8f - .14f, endX = light.x * .8f + .14f),
                Offset(light.x * .8f - .14f, -2f), Size(.28f, 4f))
        }
    }

    private fun DrawScope.plain(look: Look, t: Tone, light: Offset, shadow: Boolean) {
        val o = outline(look.shape)
        val inner = scaled(o, .8f)
        val enamel = ENAMEL[look.enamel]
        val band = ENAMEL[look.band]
        if (shadow) drop(o)
        drawPath(o, metalBrush(t, light))
        drawPath(inner, t.lo, style = Stroke(.05f, join = StrokeJoin.Round))
        drawPath(inner, enamel)
        var iconC = Offset.Zero
        var iconR = .5f
        clipPath(inner) {
            when (look.shape) {
                Shape.ROUND -> { drawCircle(band, .8f, Offset.Zero); disc(.6f, enamel, t) ; iconR = .5f }
                Shape.STAR -> { disc(.38f, band, t); iconR = .34f; iconC = Offset(0f, .02f) }
                Shape.SHIELD -> { drawRect(band, Offset(-1f, -1f), Size(2f, .52f)); drawLine(t.base, Offset(-1f, -.48f), Offset(1f, -.48f), .05f); iconC = Offset(0f, .14f); iconR = .46f }
                Shape.FLAG -> { drawPath(Path().apply { moveTo(-1f, -1f); lineTo(-.25f, -1f); lineTo(-1f, .1f); close() }, band); iconC = Offset(.12f, .0f); iconR = .48f }
                Shape.RHOMB -> { drawRect(band, Offset(-1f, .12f), Size(2f, 1f)); drawLine(t.base, Offset(-1f, .12f), Offset(1f, .12f), .05f); iconC = Offset(0f, -.18f); iconR = .32f }
                Shape.GEAR -> { disc(.5f, band, t); iconR = .4f }
                else -> Unit
            }
        }
        relief(look, t, iconC, iconR, light)
        gloss(o, light)
    }

    /** An enamel disc inside a thin metal ring. */
    private fun DrawScope.disc(r: Float, color: Color, t: Tone, c: Offset = Offset.Zero) {
        drawCircle(t.base, r + .05f, c)
        drawCircle(color, r, c)
    }

    /** The stamped picture: a deep shadow below, then the metal, lit from [light]. */
    private fun DrawScope.relief(look: Look, t: Tone, c: Offset, r: Float, light: Offset) {
        val px = look.pixels
        if (px != null && px.length >= Look.GRID * Look.GRID) {
            val cell = r * 2 / Look.GRID
            val o = Offset(c.x - r, c.y - r)
            for (i in 0 until Look.GRID * Look.GRID) {
                val col = pixelColor(px[i].digitToIntOrNull() ?: 0, t) ?: continue
                drawRect(col, Offset(o.x + (i % Look.GRID) * cell, o.y + (i / Look.GRID) * cell), Size(cell + .004f, cell + .004f))
            }
            return
        }
        val ico = look.icon?.let { Icons[it] } ?: return
        val k = r * 2 / Icons.GRID
        val line = Stroke(Icons.LINE, cap = StrokeCap.Round, join = StrokeJoin.Round)
        withTransform({ translate(c.x - r, c.y - r); scale(k, k, Offset.Zero) }) {
            translate(.7f, .9f) { drawPath(ico.body, t.deep.copy(alpha = .75f)); drawPath(ico.lines, t.deep.copy(alpha = .75f), style = line) }
            val b = Brush.linearGradient(listOf(t.hi, t.base, t.lo), start = Offset(12f + light.x * 12f, 12f + light.y * 12f), end = Offset(12f - light.x * 12f, 12f - light.y * 12f))
            drawPath(ico.body, b)
            drawPath(ico.lines, b, style = line)
        }
    }

    /** A medal on a pentagonal ribbon block, the way the old ones were worn. */
    private fun DrawScope.medal(look: Look, t: Tone, light: Offset, shadow: Boolean) {
        val block = Path().apply { moveTo(-.5f, -1.02f); lineTo(.5f, -1.02f); lineTo(.5f, -.42f); lineTo(0f, -.2f); lineTo(-.5f, -.42f); close() }
        val disc = Path().apply { addOval(Rect(Offset(0f, .36f), .62f)) }
        if (shadow) { drop(block); drop(disc) }
        clipPath(block) {
            val stripes = listOf(look.band, look.enamel, look.band, 5, look.band, look.enamel, look.band)
            val w = 1f / stripes.size
            for ((i, e) in stripes.withIndex()) drawRect(ENAMEL[e], Offset(-.5f + i * w, -1.1f), Size(w + .002f, 1f))
            drawRect(Brush.verticalGradient(listOf(Color(0x30FFFFFF), Color(0x00000000), Color(0x30000000)), -1f, -.2f), Offset(-.5f, -1.1f), Size(1f, 1f))
        }
        drawPath(block, metalBrush(t, light), style = Stroke(.05f, join = StrokeJoin.Round))
        drawRect(metalBrush(t, light), Offset(-.08f, -.3f), Size(.16f, .14f))
        drawPath(disc, metalBrush(t, light))
        drawCircle(t.lo, .52f, Offset(0f, .36f), style = Stroke(.04f))
        drawCircle(ENAMEL[look.enamel], .5f, Offset(0f, .36f))
        relief(look, t, Offset(0f, .36f), .36f, light)
        gloss(disc, light)
    }

    /** A round enamel centre inside a metal laurel wreath, tied with a ribbon below. */
    private fun DrawScope.wreath(look: Look, t: Tone, light: Offset, shadow: Boolean) {
        val body = Path().apply { addOval(Rect(Offset.Zero, .98f)) }
        if (shadow) drop(body)
        val metal = metalBrush(t, light)
        // The leaves, in pairs along two arcs.
        for (side in listOf(-1f, 1f)) {
            for (i in 0 until 7) {
                val a = (PI / 2 + side * (.35 + i * .36)).toFloat()
                val x = cos(a) * .82f; val y = sin(a) * .82f
                val deg = Math.toDegrees(a.toDouble()).toFloat() + 90f
                for ((dx, k) in listOf(-.1f to -1f, .1f to 1f)) {
                    withTransform({ translate(x, y); rotate(deg + k * side * 28f, Offset.Zero) }) {
                        drawOval(t.deep, Offset(-.07f + dx * .5f + .02f, -.17f + .03f), Size(.14f, .32f))
                        drawOval(metal, Offset(-.07f + dx * .5f, -.17f), Size(.14f, .32f))
                    }
                }
            }
        }
        drawCircle(metal, .64f, Offset.Zero)
        drawCircle(t.lo, .54f, Offset.Zero, style = Stroke(.04f))
        drawCircle(ENAMEL[look.enamel], .52f, Offset.Zero)
        // The ribbon where the branches meet.
        val bow = Path().apply { moveTo(-.34f, .92f); lineTo(0f, .78f); lineTo(.34f, .92f); lineTo(.26f, 1.06f); lineTo(0f, .9f); lineTo(-.26f, 1.06f); close() }
        drawPath(bow, ENAMEL[look.band]); drawPath(bow, t.lo, style = Stroke(.03f))
        drawPath(star(5, .13f, .06f).apply { translate(Offset(0f, -.86f)) }, metal)
        relief(look, t, Offset.Zero, .38f, light)
        gloss(Path().apply { addOval(Rect(Offset.Zero, .52f)) }, light)
    }

    /** An order: a star of faceted metal rays round an enamel centre. */
    private fun DrawScope.order(look: Look, t: Tone, light: Offset, shadow: Boolean) {
        val n = 16
        val rays = Path()
        for (i in 0 until n) {
            val a = -PI / 2 + i * 2 * PI / n
            val len = if (i % 2 == 0) 1.04 else .82
            val w = PI / n * .8
            rays.moveTo(0f, 0f)
            rays.lineTo((cos(a - w) * .45).toFloat(), (sin(a - w) * .45).toFloat())
            rays.lineTo((cos(a) * len).toFloat(), (sin(a) * len).toFloat())
            rays.lineTo((cos(a + w) * .45).toFloat(), (sin(a + w) * .45).toFloat())
            rays.close()
        }
        if (shadow) drop(rays)
        drawPath(rays, metalBrush(t, light))
        // Each ray in two facets: one half catches the light, the other is in shade.
        for (i in 0 until n) {
            val a = -PI / 2 + i * 2 * PI / n
            val len = if (i % 2 == 0) 1.04 else .82
            val w = PI / n * .8
            val half = Path().apply {
                moveTo(0f, 0f); lineTo((cos(a) * len).toFloat(), (sin(a) * len).toFloat()); lineTo((cos(a + w) * .45).toFloat(), (sin(a + w) * .45).toFloat()); close()
            }
            val lit = cos(a + .2) * light.x + sin(a + .2) * light.y > 0
            drawPath(half, if (lit) Color(0x33FFFFFF) else Color(0x33000000))
        }
        drawCircle(t.base, .56f, Offset.Zero)
        drawCircle(ENAMEL[look.band], .52f, Offset.Zero)
        drawCircle(t.base, .42f, Offset.Zero)
        drawCircle(ENAMEL[look.enamel], .39f, Offset.Zero)
        relief(look, t, Offset.Zero, .3f, light)
        gloss(Path().apply { addOval(Rect(Offset.Zero, .52f)) }, light)
    }
}
