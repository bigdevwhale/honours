package app.honours.ui.ceremony

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.honours.BuildConfig
import app.honours.container
import app.honours.data.Awarded
import app.honours.domain.Rules
import app.honours.domain.Source
import app.honours.domain.date
import app.honours.sound.Snd
import app.honours.ui.Home
import app.honours.ui.art.Badges.badge
import app.honours.ui.components.Btn
import app.honours.ui.components.Kind
import app.honours.ui.components.REST_LIGHT
import app.honours.ui.components.rememberLight
import app.honours.ui.theme.C
import app.honours.ui.theme.Mono
import app.honours.ui.theme.T
import app.honours.ui.wall.PennantView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The award ceremony on red velvet, one badge after another: a fanfare, the badge rising into the light, the
 * order typed out word by word, the stamp coming down. Then the new badges are shown on the pennant.
 */
@Composable
fun Ceremony(h: Home, awards: List<Awarded>, onDone: () -> Unit) {
    val c = LocalContext.current.container
    var i by remember { mutableIntStateOf(0) }
    var pinning by remember { mutableStateOf(false) }
    BackHandler { onDone() }
    Box(
        Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .drawBehind {
                drawRect(C.velvet)
                drawRect(Brush.radialGradient(listOf(C.velvetHi, C.velvet), center = Offset(size.width / 2, size.height * .32f), radius = size.width * .95f))
                // The velvet's pile: faint diagonal shading.
                var x = -size.height
                while (x < size.width) { drawLine(androidx.compose.ui.graphics.Color(0x0A000000), Offset(x, 0f), Offset(x + size.height, size.height), 3f); x += 9f }
            },
    ) {
        if (!pinning) {
            val a = awards[i]
            Award(h, a, i, awards.size, awards.any { it.pin != null }) {
                if (i < awards.lastIndex) i++
                else if (awards.any { it.pin != null }) { pinning = true; c.sfx.play(Snd.PIN); c.sfx.feel(25, 90) }
                else onDone()
            }
        } else {
            Pinned(h, awards, onDone)
        }
    }
}

@Composable
private fun Award(h: Home, a: Awarded, index: Int, total: Int, pins: Boolean, onNext: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    val b = h.badge(a.key) ?: run { LaunchedEffect(a.id) { onNext() }; return }
    val metal = Rules.metal(a.count)
    val still = BuildConfig.SHOTS
    val rise = remember(a.id) { Animatable(if (still) 1f else 0f) }
    val sweep = remember(a.id) { Animatable(if (still) 1f else 0f) }
    val stamp = remember(a.id) { Animatable(if (still) 1f else 0f) }
    val order = "${w.decree(b)} — ${w.resolution(b, metal)}."
    var typed by remember(a.id) { mutableIntStateOf(if (still) order.length else 0) }
    var skip by remember(a.id) { mutableStateOf(false) }
    val sensor by rememberLight()

    LaunchedEffect(a.id) {
        if (still) return@LaunchedEffect
        c.sfx.play(Snd.FANFARE)
        launch { rise.animateTo(1f, spring(dampingRatio = .55f, stiffness = Spring.StiffnessLow)) }
        launch { sweep.animateTo(1f, tween(1600)) }
        delay(900)
        if (a.up) { c.sfx.play(Snd.BELL); c.sfx.feel(30, 120) }
        while (typed < order.length) {
            if (skip) { typed = order.length; break }
            typed++
            if (typed % 3 == 0 && order[typed - 1] != ' ') c.sfx.play(Snd.TYPE, .35f, .9f + (typed % 5) * .05f)
            delay(if (order[typed - 1] == ' ') 34 else 22)
        }
        delay(250)
        c.sfx.play(Snd.STAMP); c.sfx.feel(45, 200)
        stamp.animateTo(1f, spring(dampingRatio = .45f, stiffness = Spring.StiffnessMedium))
    }

    // While the light sweeps across at the start it moves on its own; then the phone's tilt takes over.
    val light = if (sweep.value < 1f) Offset(-1f + 2f * sweep.value, -.8f + .3f * sweep.value) else sensor

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { skip = true },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            w.t("ПРИКАЗ № ${a.id} · ${w.dateY(h.now.date(h.zone))}", "ORDER No. ${a.id} · ${w.dateY(h.now.date(h.zone))}") + if (total > 1) "  ·  ${index + 1}/$total" else "",
            style = T.Label.copy(color = C.gold.copy(alpha = .8f)),
        )
        if (b.source == Source.SPECIAL) Text(w.t("ОСОБЫЙ ЗНАК КОМИССИИ", "A SPECIAL BADGE OF THE COMMISSION"), style = T.Label.copy(color = C.gold), modifier = Modifier.padding(top = 4.dp))
        Canvas(
            Modifier.padding(top = 10.dp).size(230.dp).graphicsLayer {
                val v = rise.value
                scaleX = .35f + .65f * v; scaleY = scaleX
                rotationZ = (1 - v) * -24f
                alpha = v.coerceIn(0f, 1f)
            },
        ) {
            val cc = Offset(size.width / 2, size.height / 2)
            // A halo of light behind the badge.
            drawCircle(Brush.radialGradient(listOf(C.gold.copy(alpha = .35f), C.gold.copy(alpha = 0f)), center = cc, radius = size.width * .5f), size.width * .5f, cc)
            badge(b.look, metal, cc, size.width * .36f, light)
        }
        Text(w.name(b), style = T.Award.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        Text(
            when {
                a.up -> w.t("Знак повышен до ${w.metal(metal)}!", "Raised to ${w.metal(metal)}!")
                a.count == 1 -> w.t("Вручается впервые · ${w.metal(metal)}", "First award · ${w.metal(metal)}")
                a.count == Rules.HONOURED -> w.t("Присвоено звание «Заслуженный»", "The Honoured title is conferred")
                else -> w.t("Вручается в ${a.count}-й раз · ${w.metal(metal)}", "Awarded for the ${a.count}${nth(a.count)} time · ${w.metal(metal)}")
            }.uppercase(),
            style = T.Label.copy(color = if (a.up) C.gold else C.card.copy(alpha = .7f), textAlign = TextAlign.Center), modifier = Modifier.padding(top = 4.dp),
        )
        Box(Modifier.padding(top = 16.dp).fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(C.card).padding(16.dp)) {
                Text(w.t("Наградная комиссия постановляет:", "The Awards Commission resolves:"), style = T.Small.copy(fontFamily = Mono))
                // The whole order is laid out invisibly underneath, so the paper doesn't grow while it's typed.
                Box(Modifier.padding(top = 6.dp)) {
                    val style = T.Order.copy(fontSize = 15.sp, lineHeight = 22.sp)
                    Text(order, style = style.copy(color = androidx.compose.ui.graphics.Color.Transparent))
                    Text(order.take(typed), style = style)
                }
                // Keeps the paper its final height while the words are typed, and leaves the stamp its own corner.
                Box(Modifier.height(104.dp))
            }
            if (stamp.value > 0f) Canvas(
                Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 6.dp).size(96.dp).graphicsLayer {
                    val s = 1.8f - .8f * stamp.value
                    scaleX = s; scaleY = s; alpha = stamp.value.coerceIn(0f, 1f); rotationZ = -12f
                },
            ) { Stamp.draw(this, w.t("УТВЕРЖДАЮ", "APPROVED"), w.t("ЗА МАЛЫЕ ПОДВИГИ", "FOR SMALL DEEDS")) }
        }
        Btn(
            if (index < total - 1) w.t("Следующий приказ · ${index + 2}/$total", "Next order · ${index + 2}/$total") else if (pins) w.t("Приколоть на вымпел", "Pin it to the pennant") else w.t("Служу малым подвигам!", "Proud to serve small deeds!"),
            Modifier.fillMaxWidth().padding(top = 18.dp), kind = Kind.GOLD, enabled = still || stamp.value > .5f,
        ) { onNext() }
        Text(w.t("Наклони телефон — эмаль поймает свет", "Tilt the phone: the enamel catches the light"), style = T.Small.copy(color = C.card.copy(alpha = .55f)), modifier = Modifier.padding(top = 10.dp))
    }
}

private fun nth(n: Int) = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }

/** The new badges ringed on their pennant, ready to be moved where they look best. */
@Composable
private fun Pinned(h: Home, awards: List<Awarded>, onDone: () -> Unit) {
    val w = h.w
    val light by rememberLight()
    val pins = awards.mapNotNull { it.pin }
    val pennant = pins.maxOf { it.pennant }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (awards.any { it.newPennant }) w.t("Вымпел заполнен — поднят новый!", "The pennant is full: a new one is hung!")
            else w.t("Приколото", "Pinned"),
            style = T.Award.copy(fontSize = 26.sp, textAlign = TextAlign.Center),
        )
        Text(w.t("Подержи значок и перетащи, чтобы переколоть", "Hold a badge and drag it to move it"), style = T.Small.copy(color = C.card.copy(alpha = .7f)))
        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp)).drawBehind { with(app.honours.ui.art.Pennants) { wallpaper() } }) {
            PennantView(h, pennant, if (BuildConfig.SHOTS) REST_LIGHT else light, Modifier.fillMaxSize().padding(6.dp), glow = pins.map { it.key }.toSet())
        }
        Btn(w.t("Готово", "Done"), Modifier.fillMaxWidth(), kind = Kind.GOLD) { onDone() }
    }
}
