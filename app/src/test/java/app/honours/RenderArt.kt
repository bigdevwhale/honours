package app.honours

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import app.honours.domain.Look
import app.honours.domain.Metal
import app.honours.domain.Rules
import app.honours.domain.Shape
import app.honours.domain.Specials
import app.honours.ui.art.Badges
import app.honours.ui.art.Badges.badge
import app.honours.ui.art.Icons
import app.honours.ui.art.PennantText
import app.honours.ui.art.Pennants
import app.honours.ui.art.Pennants.pennant
import app.honours.ui.art.Pennants.wallpaper
import app.honours.ui.theme.C
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

/** Draws the badges and a pennant to PNGs (run with `-Prender`): a look at the art without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArt {
    private val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }

    private fun render(name: String, w: Int, h: Int, draw: DrawScope.() -> Unit) {
        val image = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat()), draw)
        File(out, "$name.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun icons() {
        val cell = 130f
        val names = Icons.names
        render("icons", (cell * 8).toInt(), (cell * ((names.size + 7) / 8)).toInt()) {
            drawRect(C.paper)
            for ((i, n) in names.withIndex()) {
                val e = i % Look.ENAMELS
                badge(Look(Shape.ROUND, e, (e + 3) % Look.ENAMELS, n), Metal.entries[i % 3], Offset((i % 8) * cell + cell / 2, (i / 8) * cell + cell / 2), cell * .42f)
            }
        }
    }

    @Test fun shapes() {
        val cell = 170f
        val shapes = Shape.entries
        render("shapes", (cell * shapes.size).toInt(), (cell * 3).toInt()) {
            drawRect(C.felt)
            for ((i, s) in shapes.withIndex()) for ((j, m) in Metal.entries.withIndex()) {
                val e = (i + j * 2) % Look.ENAMELS
                badge(Look(s, e, (e + 5) % Look.ENAMELS, Icons.names[(i * 3 + j) % Icons.names.size]), m, Offset(i * cell + cell / 2, j * cell + cell / 2), cell * .4f)
            }
        }
    }

    @Test fun specials() {
        val cell = 170f
        val all = Specials.all
        render("specials", (cell * all.size).toInt(), cell.toInt()) {
            drawRect(C.paper)
            for ((i, b) in all.withIndex()) badge(b.look, Metal.GOLD, Offset(i * cell + cell / 2, cell / 2), cell * .4f)
        }
    }

    @Test fun pennant() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val tm = TextMeasurer(createFontFamilyResolver(ctx), Density(1f), LayoutDirection.Ltr)
        val k = 2f
        render("pennant", (Rules.PENNANT_W * k + 160).toInt(), ((Rules.PENNANT_H + Pennants.ABOVE + Pennants.BELOW) * k + 40).toInt()) {
            wallpaper()
            translate(80f, Pennants.ABOVE * k + 20f) {
                scale(k) {
                    pennant(PennantText("ЗА МАЛЫЕ ПОДВИГИ", "ВЫМПЕЛ № 1 · 2026"), tm)
                    val r = Random(3)
                    val pins = mutableListOf<app.honours.domain.Pin>()
                    repeat(Rules.PER_PENNANT) { i ->
                        val (x, y) = app.honours.domain.Engine.spot(pins, r)
                        pins += app.honours.domain.Pin(0, "k$i", x, y, 0)
                        val e = i % Look.ENAMELS
                        badge(Look(Shape.entries[i % 8], e, (e + 5) % 8, Icons.names[i * 3 % Icons.names.size]), Metal.entries[i % 3], Offset(x, y), Rules.PIN_R, tilt = (r.nextFloat() - .5f) * 10f)
                    }
                }
            }
        }
    }

    private fun DrawScope.scale(k: Float, block: DrawScope.() -> Unit) = withTransform({ scale(k, k, Offset.Zero) }, block)
}

/** The launcher icon drawn from its own vectors into docs/logo.png (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArtIcon {
    @Test fun logo() {
        val c = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        for (id in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
            ContextCompat.getDrawable(c, id)!!.apply { setBounds(0, 0, 512, 512); draw(canvas) }
        }
        File("../docs/logo.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
