package app.honours.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The stamped reliefs on the badges: one-colour pictograms on a 24-unit grid. Each is a filled body
 * (shapes joined, holes cut) plus optional lines, drawn in the metal of the badge.
 */
class Ico {
    var body = Path()
        private set
    val lines = Path()

    private fun join(p: Path) { body = Path().apply { op(body, p, PathOperation.Union) } }
    private fun cutOut(p: Path) { body = Path().apply { op(body, p, PathOperation.Difference) } }

    fun svg(d: String) = PathParser().parsePathString(d).toPath()
    fun circle(x: Float, y: Float, r: Float) = Path().apply { addOval(Rect(Offset(x, y), r)) }
    fun oval(l: Float, t: Float, r: Float, b: Float) = Path().apply { addOval(Rect(l, t, r, b)) }
    fun rect(l: Float, t: Float, r: Float, b: Float, rad: Float = 0f) = Path().apply { addRoundRect(RoundRect(l, t, r, b, CornerRadius(rad))) }
    fun star(x: Float, y: Float, r: Float, inner: Float = .45f, n: Int = 5) = Path().apply {
        for (i in 0 until n * 2) {
            val a = -PI / 2 + i * PI / n
            val rr = if (i % 2 == 0) r else r * inner
            val px = x + (cos(a) * rr).toFloat(); val py = y + (sin(a) * rr).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

    fun add(vararg p: Path) = p.forEach(::join)
    fun add(d: String) = join(svg(d))
    fun cut(vararg p: Path) = p.forEach(::cutOut)
    fun cut(d: String) = cutOut(svg(d))
    fun line(d: String) = lines.addPath(svg(d))
    fun ring(x: Float, y: Float, r: Float) = lines.addOval(Rect(Offset(x, y), r))

    /** [n] short rays around a centre, as lines. */
    fun rays(x: Float, y: Float, r1: Float, r2: Float, n: Int, turn: Double = 0.0) {
        for (i in 0 until n) {
            val a = turn + i * 2 * PI / n
            lines.moveTo(x + (cos(a) * r1).toFloat(), y + (sin(a) * r1).toFloat())
            lines.lineTo(x + (cos(a) * r2).toFloat(), y + (sin(a) * r2).toFloat())
        }
    }
}

object Icons {
    const val GRID = 24f
    /** The width of an icon's lines, in grid units. */
    const val LINE = 2f

    private val draw: Map<String, Ico.() -> Unit> = linkedMapOf(
        "kettle" to {
            add("M4.5 20 L6.5 10.5 H15.5 L17.5 20 Z"); add(rect(7.5f, 8.5f, 14.5f, 10.5f, 1f), circle(11f, 7.3f, 1.3f))
            add("M16.5 13 L21 9.5 L21.8 10.6 L17.6 16 Z"); line("M7.5 9.5 Q11 3 14.5 9.5"); cut(rect(6.6f, 15.5f, 15.4f, 16.6f))
        },
        "cup" to {
            add("M4 9 H16 V15 A5 5 0 0 1 11 20 H9 A5 5 0 0 1 4 15 Z"); add(rect(2f, 20.6f, 22f, 22.2f, .8f))
            line("M16 10.5 H17.5 A2.5 2.5 0 0 1 17.5 15.5 H16"); line("M8 2.5 Q6.5 4.5 8 6.5"); line("M12 2.5 Q10.5 4.5 12 6.5")
        },
        "pan" to {
            add(circle(9.5f, 13f, 7f)); cut(circle(9.5f, 13f, 5f)); add(circle(9.5f, 13f, 2.4f))
            add("M15.5 11.4 L22.6 9 L23.2 10.7 L16.3 14.2 Z")
        },
        "broom" to {
            line("M18 2 L11.5 12.5"); add("M9 11.5 L14.5 14.5 L12.5 22 L3 17.5 Z"); cut("M8.4 13.6 L13.4 16.3 L13.1 17.4 L7.8 14.6 Z")
            cut("M7.2 16.3 L9.8 17.6 L8.6 20.2 L6.1 19Z")
        },
        "bed" to {
            add("M2 19 V7 H4.2 V13 H22 V19 H19.8 V16.5 H4.2 V19 Z"); add(rect(5f, 10.2f, 9.6f, 12.6f, 1.2f))
            add("M10.5 10 H19.8 A2.2 2.2 0 0 1 22 12.2 V12.6 H10.5 Z")
        },
        "moon" to { add("M14.5 2.5 A9.5 9.5 0 1 0 21.5 15 A7.5 7.5 0 1 1 14.5 2.5 Z"); add(star(19f, 5f, 2.2f)) },
        "sun" to { add(circle(12f, 12f, 4.8f)); rays(12f, 12f, 7.4f, 10.4f, 8) },
        "heart" to { add("M12 21.5 C5 16 2.3 12.6 2.3 8.6 A4.9 4.9 0 0 1 12 6.6 A4.9 4.9 0 0 1 21.7 8.6 C21.7 12.6 19 16 12 21.5 Z") },
        "phone" to {
            add("M5 3.3 L8.6 2.8 L10.7 7.5 L8.4 9.3 A11 11 0 0 0 14.8 15.7 L16.6 13.4 L21.2 15.5 L20.7 19 A2.6 2.6 0 0 1 18 21.2 A15.4 15.4 0 0 1 2.8 6 A2.6 2.6 0 0 1 5 3.3 Z")
        },
        "letter" to {
            add(rect(2.5f, 5.5f, 21.5f, 18.5f, 1.6f)); cut("M3.8 7.2 L12 13 L20.2 7.2 L20.2 8.8 L12 14.6 L3.8 8.8 Z")
        },
        "book" to {
            add("M2 5 Q7 3.3 11.2 5.8 V20.2 Q7 17.8 2 19 Z"); add("M22 5 Q17 3.3 12.8 5.8 V20.2 Q17 17.8 22 19 Z")
            cut("M4 8.2 Q7 7.5 9.4 8.6 V9.4 Q7 8.4 4 9 Z"); cut("M4 11.2 Q7 10.5 9.4 11.6 V12.4 Q7 11.4 4 12 Z")
        },
        "pencil" to {
            add("M15.6 3.4 L20.6 8.4 L8.2 20.8 L3.2 20.8 L3.2 15.8 Z"); cut("M13.6 5.4 L18.6 10.4 L17.8 11.2 L12.8 6.2 Z")
            cut("M4.4 17 L7 19.6 L4.4 19.6 Z")
        },
        "shoe" to {
            add("M2 18 V10 L6 9 L8.6 12 L13.2 13 Q20 14 22 17.3 V19.6 H2 Z"); cut(rect(2f, 17.2f, 22f, 18f))
            cut(circle(8.6f, 14.4f, .7f), circle(11f, 15f, .7f))
        },
        "tree" to { add("M12 1.8 L18.2 10 H15.2 L19.8 16.4 H13.1 V22 H10.9 V16.4 H4.2 L8.8 10 H5.8 Z") },
        "leaf" to { add("M4 20.5 Q2.5 7.5 20.5 3.5 Q21 18.5 6.3 18.8 Z"); cut("M5.6 18.6 L16.2 8.2 L16.9 8.9 L6.4 19.3 Z") },
        "flower" to {
            for (i in 0 until 5) { val a = -PI / 2 + i * 2 * PI / 5; add(circle(12f + (cos(a) * 3.6).toFloat(), 8.5f + (sin(a) * 3.6).toFloat(), 2.7f)) }
            cut(circle(12f, 8.5f, 1.9f)); add(circle(12f, 8.5f, 1.1f))
            line("M12 14 V22"); add("M12 19 Q16 14.5 19.5 15.5 Q17.5 19.8 12 19 Z")
        },
        "bench" to {
            add(rect(2f, 11f, 22f, 13.2f, .5f), rect(2f, 5.8f, 22f, 8f, .5f), rect(4f, 13f, 6f, 19.5f), rect(18f, 13f, 20f, 19.5f))
            add(rect(4f, 8f, 5.6f, 11f), rect(18.4f, 8f, 20f, 11f))
        },
        "bus" to {
            add(rect(3f, 2.5f, 21f, 19f, 3f)); cut(rect(5f, 5.5f, 19f, 11f, 1f)); cut(circle(7f, 15f, 1.3f), circle(17f, 15f, 1.3f))
            add(rect(5f, 18f, 9.4f, 21.8f, 1f), rect(14.6f, 18f, 19f, 21.8f, 1f))
        },
        "bicycle" to {
            ring(6f, 16f, 4.2f); ring(18f, 16f, 4.2f)
            line("M6 16 L10 9 H17 L18 16 M10 9 L13 16 H6 M17 9 L16 5.8 H18.6 M10 9 L9.2 6.8 M7.6 6.8 H11")
        },
        "key" to {
            add(circle(7f, 12f, 4.8f)); cut(circle(7f, 12f, 1.9f)); add(rect(10.5f, 10.9f, 21.5f, 13.1f))
            add(rect(17.3f, 13f, 19.2f, 16.2f), rect(13.8f, 13f, 15.5f, 15.2f))
        },
        "door" to {
            add(rect(6.5f, 2.5f, 17.5f, 21f, .8f)); cut(rect(8.5f, 4.6f, 15.5f, 10.2f, .6f), rect(8.5f, 12f, 15.5f, 19f, .6f))
            add(circle(14.2f, 11.1f, .9f)); add(rect(4f, 21f, 20f, 22.6f))
        },
        "window" to {
            add(rect(3f, 3f, 21f, 21f, 1f))
            cut(rect(5f, 5f, 11.2f, 11.2f), rect(12.8f, 5f, 19f, 11.2f), rect(5f, 12.8f, 11.2f, 19f), rect(12.8f, 12.8f, 19f, 19f))
        },
        "plant" to {
            add("M6.2 14.5 H17.8 L16.4 22 H7.6 Z"); add(rect(5f, 12.6f, 19f, 14.8f, .6f))
            add("M11.6 12.8 Q5.5 11.5 4.6 4 Q10.8 5 11.6 12.8 Z"); add("M12.4 12.8 Q13 5.4 20 3.2 Q19.8 10.4 12.4 12.8 Z")
        },
        "glass" to {
            add("M5 2.8 H19 L17.2 20.4 A1.6 1.6 0 0 1 15.6 22 H8.4 A1.6 1.6 0 0 1 6.8 20.4 Z"); cut("M7 4.8 H17 L16.5 10 H7.5 Z")
        },
        "apple" to {
            add("M12 7.5 C9 5.4 3.8 6 3.8 12 C3.8 17.6 8 22.2 10.5 21.6 C11.3 21.4 12.7 21.4 13.5 21.6 C16 22.2 20.2 17.6 20.2 12 C20.2 6 15 5.4 12 7.5 Z")
            line("M12 7.4 Q12 4.4 13.6 2.6"); add("M13.4 5.4 Q15.4 1.8 19.4 2.4 Q17.8 6 13.4 5.4 Z"); cut("M6.4 11.2 Q7 9 9 8.6 L9.2 9.6 Q7.8 10 7.4 11.6 Z")
        },
        "bread" to {
            add("M3 12 A5 4.2 0 0 1 8 7 H16 A5 4.2 0 0 1 21 12 V19 A1.5 1.5 0 0 1 19.5 20.5 H4.5 A1.5 1.5 0 0 1 3 19 Z")
            cut("M7.4 9.6 L8.6 10.3 L6.8 13.8 L5.6 13.1 Z"); cut("M12 9.6 L13.2 10.3 L11.4 13.8 L10.2 13.1 Z"); cut("M16.6 9.6 L17.8 10.3 L16 13.8 L14.8 13.1 Z")
        },
        "fork" to {
            add(rect(4.4f, 2f, 5.6f, 8.5f), rect(6.9f, 2f, 8.1f, 8.5f), rect(9.4f, 2f, 10.6f, 8.5f))
            add("M4.4 7.5 H10.6 V9 A3 3 0 0 1 8.6 11.8 V22 H6.4 V11.8 A3 3 0 0 1 4.4 9 Z")
            add("M15 2 C18.6 3 19.2 8 19.2 12.4 H17.2 V22 H15 Z")
        },
        "drop" to {
            add("M12 2.2 C12 2.2 4.8 10.5 4.8 15 A7.2 7.2 0 0 0 19.2 15 C19.2 10.5 12 2.2 12 2.2 Z")
            cut("M8.2 15 A3.8 3.8 0 0 0 12 18.8 V17.4 A2.4 2.4 0 0 1 9.6 15 Z")
        },
        "sock" to {
            add("M8 2 H15 V12.5 L19.6 16.4 A3.1 3.1 0 0 1 15.4 21.1 L9 15.6 A3 3 0 0 1 8 13.3 Z")
            cut(rect(8f, 4.6f, 15f, 5.8f), rect(8f, 7f, 15f, 8.2f))
        },
        "jar" to {
            add(rect(6.5f, 2f, 17.5f, 5.2f, 1f))
            add("M6 6 H18 A1 1 0 0 1 19 7 V20 A2 2 0 0 1 17 22 H7 A2 2 0 0 1 5 20 V7 A1 1 0 0 1 6 6 Z")
            cut(rect(7.5f, 11f, 16.5f, 17.2f, 1f)); add(oval(9.6f, 12.4f, 14.4f, 15.8f))
        },
        "umbrella" to {
            add("M2 12.2 A10 9.2 0 0 1 22 12.2 Q20.3 10.6 18.6 12.2 Q17 10.6 15.3 12.2 Q13.6 10.6 12 12.2 Q10.4 10.6 8.7 12.2 Q7 10.6 5.4 12.2 Q3.7 10.6 2 12.2 Z")
            add(rect(11.3f, 1.4f, 12.7f, 3.6f)); line("M12 11.5 V19 A2 2 0 0 1 8 19")
        },
        "snow" to {
            rays(12f, 12f, 0f, 10f, 6, -PI / 2)
            for (i in 0 until 6) {
                val a = -PI / 2 + i * PI / 3
                val bx = 12 + cos(a) * 6.2; val by = 12 + sin(a) * 6.2
                for (s in listOf(-1, 1)) {
                    val b = a + s * PI / 4
                    lines.moveTo(bx.toFloat(), by.toFloat()); lines.lineTo((bx + cos(b) * 3).toFloat(), (by + sin(b) * 3).toFloat())
                }
            }
        },
        "wave" to {
            line("M1.5 8.5 Q4.6 5 7.6 8.5 T13.6 8.5 T19.6 8.5 L22.5 7.4"); line("M1.5 14 Q4.6 10.5 7.6 14 T13.6 14 T19.6 14 L22.5 12.9")
            line("M1.5 19.5 Q4.6 16 7.6 19.5 T13.6 19.5 T19.6 19.5 L22.5 18.4")
        },
        "bird" to {
            add("M3.6 15 C3.6 9 9 7.4 12.6 9.4 L14 6 C15.4 3.4 19 3.4 20 6 L22.8 7 L20.3 8.3 C20 13 17 18 11 18 L5.6 21.2 L7 17.4 C4.8 17 3.6 16.3 3.6 15 Z")
            cut(circle(17.3f, 6.3f, .9f)); cut("M7 13 Q10.5 11 14 13.6 Q10.5 13.4 7.6 14.4 Z")
        },
        "cat" to {
            add("M4 6.2 L4 2.2 L8.6 5.4 Q12 4.4 15.4 5.4 L20 2.2 L20 6.2 Q21.6 9 21.1 13 Q20 20.4 12 20.8 Q4 20.4 2.9 13 Q2.4 9 4 6.2 Z")
            cut(oval(7.2f, 10f, 10f, 13f), oval(14f, 10f, 16.8f, 13f)); cut("M10.9 14.6 H13.1 L12 15.9 Z")
            cut("M12 15.9 L12.4 16.9 L11.6 16.9 Z")
        },
        "hand" to {
            add(rect(6.9f, 4.4f, 9.4f, 13f, 1.25f), rect(9.9f, 2.5f, 12.4f, 12f, 1.25f), rect(12.9f, 3f, 15.4f, 12f, 1.25f), rect(15.9f, 5f, 18.4f, 13f, 1.25f))
            add("M6.9 10.6 H18.4 V15.2 A7 7 0 0 1 11.4 22 A6 6 0 0 1 6.2 19 L2.8 13.6 A1.7 1.7 0 0 1 5.5 11.8 L6.9 13.8 Z")
        },
        "eye" to { add("M1.5 12 Q12 1.8 22.5 12 Q12 22.2 1.5 12 Z"); cut(circle(12f, 12f, 4.6f)); add(circle(12f, 12f, 2.8f)); cut(circle(13.1f, 10.9f, .9f)) },
        "note" to {
            add(oval(3.8f, 15f, 10.2f, 20.2f), oval(13.8f, 13f, 20.2f, 18.2f)); add(rect(8.4f, 4f, 10.2f, 17.6f), rect(18.4f, 2f, 20.2f, 15.6f))
            add("M8.4 4 L20.2 1.8 V5 L8.4 7.2 Z")
        },
        "clock" to {
            add(circle(12f, 12f, 10f)); cut(circle(12f, 12f, 8.1f)); add(rect(11f, 5.6f, 13f, 13f, 1f), rect(11f, 11f, 17f, 13f, 1f))
            for (i in 0 until 4) { val a = i * PI / 2; add(circle(12f + (cos(a) * 6.3).toFloat(), 12f + (sin(a) * 6.3).toFloat(), .75f)) }
        },
        "candle" to {
            add(rect(8f, 10.4f, 16f, 22f, 1f)); add("M12 1.8 C14.6 4.8 15.1 7 12 9.1 C8.9 7 9.4 4.8 12 1.8 Z"); add(rect(11.6f, 8.8f, 12.4f, 10.6f))
            cut("M8 12.4 H11 V15.4 A1 1 0 0 1 9 15.4 V13.4 H8 Z")
        },
        "star" to { add(star(12f, 12.6f, 10.6f, .44f)) },
        "house" to { add("M12 2.4 L22.2 11 H19 V21 H5 V11 H1.8 Z"); cut(rect(10f, 14f, 14f, 21f, .4f), rect(6.8f, 12f, 9.2f, 14.4f), rect(14.8f, 12f, 17.2f, 14.4f)) },
        "gift" to {
            add(rect(4f, 10.5f, 20f, 21.5f, .6f), rect(3f, 7f, 21f, 10.6f, .6f)); cut(rect(11.1f, 7f, 12.9f, 21.5f), rect(3f, 10.3f, 21f, 11.1f))
            add("M12 7 C9.2 1.8 4.6 3.6 7 7 Z"); add("M12 7 C14.8 1.8 19.4 3.6 17 7 Z")
        },
        "camera" to {
            add(rect(2f, 7f, 22f, 20f, 2.2f)); add("M8 7.4 L9.6 4 H14.4 L16 7.4 Z"); cut(circle(12f, 13.5f, 4.7f)); add(circle(12f, 13.5f, 2.9f))
            cut(rect(17.6f, 9f, 19.8f, 10.6f, .4f))
        },
        "mountain" to {
            add("M1.5 20.5 L9 7 L13 13.6 L15.6 10 L22.5 20.5 Z"); cut("M6.9 10.8 L9 7 L11.1 10.6 L10 10.1 L9 11.1 L8 10.2 Z")
            add(circle(18.4f, 5f, 2.3f))
        },
        "coin" to {
            add(circle(12f, 12f, 9.8f)); cut(circle(12f, 12f, 8.1f)); add(circle(12f, 12f, 6.8f))
            cut(rect(11f, 8f, 13f, 16.2f)); cut("M11 8 L8.8 10.2 L8.8 11.6 L11 9.6 Z"); cut(rect(9.2f, 15f, 14.8f, 16.4f))
        },
        "shirt" to { add("M8 3 L2.8 6.2 L5 11.2 L7 10.2 V21.4 H17 V10.2 L19 11.2 L21.2 6.2 L16 3 Q12 6.6 8 3 Z"); cut(circle(12f, 10.5f, .7f), circle(12f, 14f, .7f), circle(12f, 17.5f, .7f)) },
        "tooth" to {
            add("M7 3 C9 3 10 4 12 4 C14 4 15 3 17 3 C20 3 21.2 6 20.6 9 C20 12 18.6 13 18 16 L17 21 C16.7 22.3 15 22.3 14.8 21 L13.5 16 C13.2 14.8 10.8 14.8 10.5 16 L9.2 21 C9 22.3 7.3 22.3 7 21 L6 16 C5.4 13 4 12 3.4 9 C2.8 6 4 3 7 3 Z")
            cut("M6 7.4 Q6.4 5.4 8.4 5.2 L8.5 6.3 Q7.3 6.5 7.1 7.6 Z")
        },
        "stairs" to { add("M2 21.5 V17 H7 V12.5 H12 V8 H17 V3.5 H22 V21.5 Z") },
        "chair" to { add("M6 2 H8.2 V11 H18.4 V13.2 H17.8 V21.6 H15.6 V15 H8.2 V21.6 H6 Z") },
        "bulb" to {
            add("M12 1.8 A7.2 7.2 0 0 1 16.2 14.8 V17.2 H7.8 V14.8 A7.2 7.2 0 0 1 12 1.8 Z"); add(rect(8f, 18f, 16f, 19.6f, .6f), rect(9.2f, 20.4f, 14.8f, 22.2f, .9f))
            cut("M10 14.4 V10 L12 12 L14 10 V14.4 H13.2 V12.2 L12 13.4 L10.8 12.2 V14.4 Z")
        },
        "bag" to { add("M4 8.4 H20 L19 22 H5 Z"); line("M8.6 10.4 V6.6 A3.4 3.4 0 0 1 15.4 6.6 V10.4"); cut(circle(8.6f, 10.6f, .9f), circle(15.4f, 10.6f, .9f)) },
        "ticket" to {
            add("M2 6.6 H22 V10 A2 2 0 0 0 22 14 V17.4 H2 V14 A2 2 0 0 0 2 10 Z")
            cut(rect(15f, 7.8f, 16f, 9.4f), rect(15f, 11.2f, 16f, 12.8f), rect(15f, 14.6f, 16f, 16.2f)); cut(star(8.8f, 12f, 3.4f))
        },
    )

    val names: List<String> = draw.keys.toList()

    private val cache = HashMap<String, Ico>()

    operator fun get(name: String): Ico? = cache[name] ?: draw[name]?.let { f -> Ico().apply(f).also { cache[name] = it } }
}
