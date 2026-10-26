package app.honours.ui.wall

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.honours.R
import app.honours.domain.Badge
import app.honours.domain.Metal
import app.honours.ui.Words
import app.honours.ui.art.Badges.badge
import app.honours.ui.theme.C
import java.io.File
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin

/**
 * The award sheet sent to someone else: a printed form with a red and gold border, the badge, the order
 * typed out, the chairman's signature and a round blue stamp. 1080 × 1520, shared as a PNG.
 */
object AwardSheet {
    const val W = 1080
    const val H = 1520

    fun draw(ctx: Context, w: Words, b: Badge, metal: Metal, to: String, from: String, date: LocalDate): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val narrow = ResourcesCompat.getFont(ctx, R.font.ptsans_narrow_bold) ?: Typeface.DEFAULT_BOLD
        val sans = ResourcesCompat.getFont(ctx, R.font.ptsans_regular) ?: Typeface.DEFAULT
        val mono = ResourcesCompat.getFont(ctx, R.font.ptmono) ?: Typeface.MONOSPACE
        val red = C.red.toArgb()
        val gold = C.goldDeep.toArgb()
        val ink = C.ink.toArgb()

        cv.drawColor(0xFFF7EFDC.toInt())
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // The border: a red band, a thin gold rule inside it, and stars in the corners.
        p.style = Paint.Style.STROKE
        p.color = red; p.strokeWidth = 16f; cv.drawRect(34f, 34f, W - 34f, H - 34f, p)
        p.color = gold; p.strokeWidth = 4f; cv.drawRect(60f, 60f, W - 60f, H - 60f, p)
        p.style = Paint.Style.FILL
        for ((x, y) in listOf(60f to 60f, W - 60f to 60f, 60f to H - 60f, W - 60f to H - 60f)) { p.color = 0xFFF7EFDC.toInt(); cv.drawCircle(x, y, 20f, p); star(cv, x, y, 18f, red) }

        fun text(s: String, tf: Typeface, size: Float, color: Int, y: Float, width: Int = W - 220, spacing: Float = 0f): Float {
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tf; textSize = size; this.color = color; letterSpacing = spacing }
            val l = StaticLayout.Builder.obtain(s, 0, s.length, tp, width).setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, 1.12f).build()
            cv.save(); cv.translate((W - width) / 2f, y); l.draw(cv); cv.restore()
            return y + l.height
        }

        var y = text(w.t("НАГРАДНОЙ ЛИСТ", "AWARD SHEET"), narrow, 88f, red, 120f, spacing = .08f)
        y = text(w.t("Наградная комиссия по малым подвигам", "The Commission for Small Deeds"), mono, 30f, ink, y + 6f)

        // The badge itself, drawn by the same code as on the pennant.
        val r = 140f
        val cy = y + 46f + r
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(cv), Size(W.toFloat(), H.toFloat())) {
            badge(b.look, metal, Offset(W / 2f, cy), r)
        }
        y = cy + r + 40f
        y = text(w.t("Настоящим удостоверяется, что", "This is to certify that"), sans, 36f, ink, y)
        y = text(to.ifBlank { w.t("товарищ", "comrade") }, narrow, 84f, ink, y + 8f)
        y = text(w.t("награждается", "is hereby awarded"), sans, 36f, ink, y + 8f)
        y = text(w.with(b).replaceFirstChar { it.uppercase() }, narrow, 60f, red, y + 4f)
        y = text(w.decree(b), mono, 33f, ink, y + 24f, W - 260)

        // Date, signature, stamp.
        val base = H - 230f
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = sans; textSize = 30f; color = ink }
        cv.drawText(w.dateY(date), 130f, base, tp)
        p.color = ink; p.strokeWidth = 2f
        cv.drawLine(560f, base + 4f, W - 130f, base + 4f, p)
        tp.textSize = 26f; tp.color = C.muted.toArgb()
        cv.drawText(w.t("Председатель комиссии", "Chairman of the Commission"), 560f, base + 40f, tp)
        val sign = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(narrow, Typeface.ITALIC); textSize = 52f; color = 0xFF233A80.toInt() }
        cv.drawText(from.ifBlank { w.t("Значкист", "Small Honours") }, 590f, base - 12f, sign)
        // The stamp goes over the signature, as stamps do.
        stamp(cv, W - 250f, base - 30f, w, mono)
        return bmp
    }

    private fun star(cv: Canvas, x: Float, y: Float, r: Float, color: Int) {
        val path = android.graphics.Path()
        for (i in 0 until 10) {
            val a = -Math.PI / 2 + i * Math.PI / 5
            val rr = if (i % 2 == 0) r else r * .45f
            val px = x + (cos(a) * rr).toFloat(); val py = y + (sin(a) * rr).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        cv.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    /** A round rubber stamp, slightly crooked and not quite fully inked. */
    private fun stamp(cv: Canvas, x: Float, y: Float, w: Words, mono: Typeface) {
        val blue = 0xB03B57A8.toInt()
        cv.save(); cv.rotate(-14f, x, y)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = blue; strokeWidth = 6f }
        cv.drawCircle(x, y, 110f, p); p.strokeWidth = 3f; cv.drawCircle(x, y, 78f, p)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = 22f; color = blue; textAlign = Paint.Align.CENTER }
        val ring = android.graphics.Path().apply { addCircle(x, y, 88f, android.graphics.Path.Direction.CW) }
        cv.drawTextOnPath(w.t("★ НАГРАДНАЯ КОМИССИЯ ★ ЗА МАЛЫЕ ПОДВИГИ ", "★ COMMISSION FOR SMALL DEEDS ★ AWARDS DEPT ").uppercase(), ring, 0f, 0f, tp)
        star(cv, x, y - 28f, 22f, blue)
        tp.textSize = 26f
        cv.drawText(w.t("УТВЕРЖДЕНО", "APPROVED"), x, y + 18f, tp)
        cv.restore()
    }

    /** Writes the sheet to the cache and opens the share sheet for it. */
    fun share(ctx: Context, bmp: Bitmap, title: String) {
        val dir = File(ctx.cacheDir, "sheets").apply { mkdirs() }
        val f = File(dir, "award.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        val send = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
