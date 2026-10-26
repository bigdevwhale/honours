package app.honours.ui.art

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.honours.domain.Rules
import app.honours.ui.theme.C
import app.honours.ui.theme.Narrow
import kotlin.math.hypot
import kotlin.random.Random

/** What the felt says: the motto across the top and the pennant's number and year under it. */
data class PennantText(val motto: String, val sub: String)

/**
 * A felt pennant hanging from a gilded rod on a cord: red cloth with a fine nap, gold borders and lettering,
 * a gold fringe along the point and a tassel at its tip. Drawn in pennant units, 300 × 440, from (0, 0).
 */
object Pennants {
    const val W = Rules.PENNANT_W
    const val H = Rules.PENNANT_H
    /** Room the cord and the tassel need above and below the felt. */
    const val ABOVE = 64f
    const val BELOW = 58f

    val felt: Path = Path().apply { moveTo(0f, 0f); lineTo(W, 0f); lineTo(W, 320f); lineTo(W / 2, H); lineTo(0f, 320f); close() }

    private fun inset(d: Float) = Path().apply {
        // The point's slope is 120/150, so the inset along it is a bit longer vertically.
        val k = d * hypot(150f, 120f) / 150f
        moveTo(d, d); lineTo(W - d, d); lineTo(W - d, 320f - d * .45f); lineTo(W / 2, H - k); lineTo(d, 320f - d * .45f); close()
    }

    private val border1 = inset(9f)
    private val border2 = inset(14f)

    /** The nap of the felt: fixed specks, so it doesn't shimmer between frames. */
    private val nap: List<Triple<Float, Float, Boolean>> = Random(7).let { r -> List(900) { Triple(r.nextFloat() * W, r.nextFloat() * H, r.nextBoolean()) } }

    fun DrawScope.pennant(text: PennantText, tm: TextMeasurer, cloth: Color = C.felt, clothDeep: Color = C.feltDeep) {
        // The cord, from a nail above to both ends of the rod.
        val nail = Offset(W / 2, -ABOVE + 8f)
        for (end in listOf(Offset(-4f, -8f), Offset(W + 4f, -8f))) {
            drawLine(Color(0xFF5B3A1E), nail, end, 2.6f, StrokeCap.Round)
            drawLine(Color(0x40FFFFFF), nail + Offset(0f, -.6f), end + Offset(0f, -.6f), .8f)
        }
        drawCircle(Color(0x33000000), 5.5f, nail + Offset(1.5f, 2.5f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFE5E2DC), Color(0xFF77736C)), center = nail - Offset(1.5f, 1.5f), radius = 6f), 4.6f, nail)

        // The felt with its own shadow on the wall.
        drawPath(felt.let { Path().apply { addPath(it, Offset(5f, 8f)) } }, Color(0x2A000000))
        drawPath(felt, Brush.verticalGradient(listOf(cloth, clothDeep), 0f, H))
        clipPath(felt) {
            for ((x, y, light) in nap) drawCircle(if (light) Color(0x14FFFFFF) else Color(0x18000000), .9f, Offset(x, y))
            // Soft folds where it hangs from the rod.
            drawRect(Brush.horizontalGradient(listOf(Color(0x22000000), Color(0x00000000), Color(0x10FFFFFF), Color(0x00000000), Color(0x26000000)), 0f, W), Offset.Zero, Size(W, H))
        }
        drawPath(border1, C.gold, style = Stroke(2.6f))
        drawPath(border2, C.gold.copy(alpha = .7f), style = Stroke(1f))

        // The fringe along both edges of the point.
        for (side in listOf(-1f, 1f)) {
            var t = 0f
            while (t <= 1f) {
                val x = W / 2 + side * (W / 2) * (1 - t)
                val y = 320f + 120f * t
                drawLine(C.goldDeep, Offset(x, y), Offset(x + side * 1.5f, y + 13f), 1.3f, StrokeCap.Round)
                drawLine(C.gold, Offset(x - side * .6f, y), Offset(x + side * .9f, y + 12f), .9f, StrokeCap.Round)
                t += .022f
            }
        }
        // The tassel at the tip.
        drawLine(C.goldDeep, Offset(W / 2, H - 2f), Offset(W / 2, H + 14f), 2f)
        drawCircle(C.gold, 4.2f, Offset(W / 2, H + 16f))
        val tassel = Path().apply { moveTo(W / 2 - 4f, H + 18f); lineTo(W / 2 + 4f, H + 18f); lineTo(W / 2 + 9f, H + BELOW - 6f); lineTo(W / 2 - 9f, H + BELOW - 6f); close() }
        drawPath(tassel, C.goldDeep)
        for (i in -4..4) drawLine(C.gold, Offset(W / 2 + i * .8f, H + 19f), Offset(W / 2 + i * 2f, H + BELOW - 7f), .8f)

        // The rod, gilded, with round finials.
        drawRoundRect(Color(0x33000000), Offset(-14f, -12f + 3f), Size(W + 28f, 10f), CornerRadius(5f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFE9A6), C.gold, C.goldDeep), -14f, -2f), Offset(-14f, -14f), Size(W + 28f, 10f), CornerRadius(5f))
        for (x in listOf(-16f, W + 16f)) drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF0B8), C.goldDeep), center = Offset(x - 2f, -11f), radius = 9f), 7f, Offset(x, -9f))

        // The lettering.
        val motto = tm.measure(text.motto, TextStyle(fontFamily = Narrow, fontSize = 23f.toSp(), letterSpacing = 1.5f.toSp(), color = C.gold, textAlign = TextAlign.Center), constraints = Constraints.fixedWidth((W - 40).toInt().coerceAtLeast(1)))
        drawText(motto, topLeft = Offset(20f, 26f))
        val sub = tm.measure(text.sub, TextStyle(fontFamily = Narrow, fontSize = 13f.toSp(), letterSpacing = 2f.toSp(), color = C.gold.copy(alpha = .85f), textAlign = TextAlign.Center), constraints = Constraints.fixedWidth((W - 40).toInt()))
        val subY = 26f + motto.size.height + 6f
        drawText(sub, topLeft = Offset(20f, subY))
        val lineY = subY + sub.size.height + 6f
        drawLine(C.gold, Offset(60f, lineY), Offset(W - 60f, lineY), 1.2f)
        for (i in -1..1) drawPath(star(W / 2 + i * 16f, lineY, if (i == 0) 5f else 3.2f), C.gold)
    }

    private fun star(x: Float, y: Float, r: Float) = Path().apply {
        for (i in 0 until 10) {
            val a = -Math.PI / 2 + i * Math.PI / 5
            val rr = if (i % 2 == 0) r else r * .45f
            val px = x + (Math.cos(a) * rr).toFloat(); val py = y + (Math.sin(a) * rr).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

    /** Wallpaper behind the pennants: faded stripes and a small diamond, like an old office corridor. */
    fun DrawScope.wallpaper() {
        drawRect(C.wall)
        val step = 36.dp.toPx()
        val d3 = 3.dp.toPx()
        val d2 = 2.dp.toPx()
        var x = 0f
        while (x < size.width) { drawRect(C.wallStripe, Offset(x, 0f), Size(step * .32f, size.height)); x += step }
        var y = step / 2
        var row = 0
        while (y < size.height) {
            var xx = step * .66f + if (row % 2 == 0) 0f else step / 2
            while (xx < size.width) {
                drawPath(Path().apply { moveTo(xx, y - d3); lineTo(xx + d2, y); lineTo(xx, y + d3); lineTo(xx - d2, y); close() }, C.wallStripe)
                xx += step
            }
            y += step / 2; row++
        }
    }
}
