package app.honours.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.honours.R

/** Office paper and typewriter ink, red felt with gold thread, and the velvet of an award box. */
@Immutable
object C {
    val paper = Color(0xFFF2EAD8)
    val paperDark = Color(0xFFE6DAC0)
    val card = Color(0xFFFBF6EA)
    val line = Color(0xFFD8C9AC)
    val ink = Color(0xFF2B2420)
    val muted = Color(0xFF85786A)
    val red = Color(0xFFB3261E)
    val redDeep = Color(0xFF8E1A14)
    val felt = Color(0xFFC1272D)
    val feltDeep = Color(0xFF8F1A1C)
    val gold = Color(0xFFE2BC55)
    val goldDeep = Color(0xFFA9832A)
    val velvet = Color(0xFF4A0D12)
    val velvetHi = Color(0xFF8A1C24)
    val wall = Color(0xFFE9DEC6)
    val wallStripe = Color(0xFFE1D4B8)
    val wood = Color(0xFF7A4E2A)
    val stamp = Color(0xFF3B57A8)
    val night = Color(0xE01A1210)
}

val Sans = FontFamily(Font(R.font.ptsans_regular, FontWeight.Normal), Font(R.font.ptsans_bold, FontWeight.Bold))
/** Headings and everything lettered on the pennants. */
val Narrow = FontFamily(Font(R.font.ptsans_narrow_bold, FontWeight.Bold))
/** The typewriter of the commission's orders. */
val Mono = FontFamily(Font(R.font.ptmono, FontWeight.Normal))

object T {
    val Title = TextStyle(fontFamily = Narrow, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 30.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Narrow, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val BodyB = Body.copy(fontWeight = FontWeight.Bold)
    val Small = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp, color = C.muted)
    val Label = TextStyle(fontFamily = Narrow, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp, color = C.muted)
    val Button = TextStyle(fontFamily = Narrow, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp, letterSpacing = .4.sp)
    val Order = TextStyle(fontFamily = Mono, fontSize = 14.sp, lineHeight = 20.sp, color = C.ink)
    val Award = TextStyle(fontFamily = Narrow, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 33.sp, color = C.gold)
}

@Composable
fun HonoursTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = C.red, onPrimary = Color.White, background = C.paper, surface = C.card, onSurface = C.ink,
            onBackground = C.ink, outline = C.line, error = C.red,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
