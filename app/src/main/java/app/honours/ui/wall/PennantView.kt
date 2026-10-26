package app.honours.ui.wall

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import app.honours.container
import app.honours.domain.Pin
import app.honours.domain.Rules
import app.honours.domain.date
import app.honours.sound.Snd
import app.honours.ui.Home
import app.honours.ui.art.Badges.badge
import app.honours.ui.art.PennantText
import app.honours.ui.art.Pennants
import app.honours.ui.art.Pennants.pennant
import app.honours.ui.theme.C
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/** Where the pennant sits in a box: its scale and the top-left corner of the felt. */
private class Fit(size: Size) {
    val k = min(size.width / (Pennants.W + 50f), size.height / (Pennants.H + Pennants.ABOVE + Pennants.BELOW))
    val ox = (size.width - Pennants.W * k) / 2
    val oy = (size.height - (Pennants.H + Pennants.ABOVE + Pennants.BELOW) * k) / 2 + Pennants.ABOVE * k
    fun toFelt(p: Offset) = Offset((p.x - ox) / k, (p.y - oy) / k)
}

/**
 * One pennant with its badges. Tap a badge to open it; press and hold to move it. [glow] rings a badge
 * just awarded, and badges sway a little as the phone tilts.
 */
@Composable
fun PennantView(h: Home, index: Int, light: Offset, modifier: Modifier = Modifier, glow: Set<String> = emptySet(), onTap: (String) -> Unit = {}) {
    val c = LocalContext.current.container
    val tm = rememberTextMeasurer()
    val pins = h.s.pinsOn(index)
    val year = (pins.minOfOrNull { it.at } ?: h.now).date(h.zone).year
    val text = PennantText(h.w.motto, h.w.pennant(index, year))
    var drag by remember { mutableStateOf<Pair<String, Offset>?>(null) }
    var size by remember { mutableStateOf(Size.Zero) }

    fun hit(p: Offset): Pin? {
        val f = Fit(size).toFelt(p)
        return pins.filter { hypot(it.x - f.x, it.y - f.y) < Rules.PIN_R * 1.25f }.minByOrNull { hypot(it.x - f.x, it.y - f.y) }
    }

    Canvas(
        modifier
            .pointerInput(pins) { detectTapGestures { p -> hit(p)?.let { onTap(it.key) } } }
            .pointerInput(pins) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { p -> hit(p)?.let { pin -> drag = pin.key to Offset(pin.x, pin.y); c.sfx.feel(20, 60) } },
                    onDrag = { ch, d -> ch.consume(); drag = drag?.let { (k, o) -> k to (o + d / Fit(size).k) } },
                    onDragEnd = {
                        drag?.let { (k, o) -> c.write { repo.move(index, k, o.x, o.y) }; c.sfx.play(Snd.PIN, .6f) }
                        drag = null
                    },
                    onDragCancel = { drag = null },
                )
            },
    ) {
        size = this.size
        val f = Fit(this.size)
        translate(f.ox, f.oy) {
            withTransform({ scale(f.k, f.k, Offset.Zero) }) {
                pennant(text, tm)
                for (p in pins.sortedBy { if (it.key == drag?.first) 1 else 0 }) {
                    val b = h.badge(p.key) ?: continue
                    val at = if (p.key == drag?.first) drag!!.second else Offset(p.x, p.y)
                    val lift = p.key == drag?.first
                    // A badge hangs from its pin above the centre, so it swings with the tilt, each a little differently.
                    val tilt = light.x * -7f + ((abs(p.key.hashCode()) % 9) - 4) * .8f
                    if (p.key in glow) drawCircle(C.gold.copy(alpha = .35f), Rules.PIN_R * 1.45f, at)
                    if (p.key in glow) drawCircle(C.gold, Rules.PIN_R * 1.45f, at, style = Stroke(1.6f))
                    // The pin's shadow on the felt.
                    drawCircle(Color(0x26000000), Rules.PIN_R * .95f, at + Offset(2.5f, 4f))
                    badge(b.look, h.s.metal(p.key), at, Rules.PIN_R * if (lift) 1.15f else 1f, light, shadow = true, tilt = tilt)
                }
            }
        }
    }
}
