package app.honours.ui.ceremony

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import app.honours.ui.theme.C
import kotlin.math.cos
import kotlin.math.sin

/** The commission's round blue stamp: two rings, a star, a word across the middle and a motto round the edge. */
object Stamp {
    fun draw(d: DrawScope, word: String, motto: String) = with(d) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2 * .94f
        val ink = C.stamp.copy(alpha = .85f)
        drawCircle(ink, r, c, style = Stroke(r * .06f))
        drawCircle(ink, r * .72f, c, style = Stroke(r * .03f))
        val star = Path().apply {
            for (i in 0 until 10) {
                val a = -Math.PI / 2 + i * Math.PI / 5
                val rr = if (i % 2 == 0) r * .2f else r * .09f
                val x = c.x + (cos(a) * rr).toFloat(); val y = c.y - r * .32f + (sin(a) * rr).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(star, ink)
        drawIntoCanvas { cv ->
            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = ink.toArgb(); textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.MONOSPACE
                isFakeBoldText = true; textSize = r * .2f
            }
            cv.nativeCanvas.drawText(word, c.x, c.y + r * .12f, p)
            p.textSize = r * .15f; p.isFakeBoldText = false
            val ring = android.graphics.Path().apply { addCircle(c.x, c.y, r * .8f, android.graphics.Path.Direction.CW) }
            val text = "★ $motto ★ $motto "
            cv.nativeCanvas.drawTextOnPath(text, ring, 0f, r * .05f, p)
        }
    }
}
