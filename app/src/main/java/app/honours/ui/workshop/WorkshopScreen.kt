package app.honours.ui.workshop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.honours.container
import app.honours.domain.Look
import app.honours.domain.Metal
import app.honours.domain.Own
import app.honours.domain.Registry
import app.honours.domain.Shape
import app.honours.domain.date
import app.honours.ui.Home
import app.honours.ui.Nav
import app.honours.ui.Overlay
import app.honours.ui.art.Badges
import app.honours.ui.art.Badges.badge
import app.honours.ui.art.Icons
import app.honours.ui.components.BadgeIcon
import app.honours.ui.components.Btn
import app.honours.ui.components.Card
import app.honours.ui.components.Chip
import app.honours.ui.components.Field
import app.honours.ui.components.Flow
import app.honours.ui.components.Gap
import app.honours.ui.components.Kind
import app.honours.ui.components.Label
import app.honours.ui.components.LocalToast
import app.honours.ui.components.Sheet
import app.honours.ui.components.TopBar
import app.honours.ui.components.rememberLight
import app.honours.ui.theme.C
import app.honours.ui.theme.T

/** The workshop: badges of your own for deeds the list doesn't have, and award sheets for other people. */
@Composable
fun WorkshopScreen(h: Home, nav: Nav) {
    val w = h.w
    Column(Modifier.fillMaxSize()) {
        TopBar(w.t("Мастерская", "Workshop"), w.t("свои знаки и награды другим", "your own badges, and honours for others"))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Card {
                Text(w.t("Учредить свой знак", "Institute a badge of your own"), style = T.Heading)
                Text(w.t("Для подвигов, которых нет в перечне: «Не купил{|а} третий свитер», «Дочитал{|а} инструкцию». Форма, эмаль, рисунок и указ — твои.",
                    "For deeds the list doesn't have: \"Didn't buy a third sweater\", \"Read the manual to the end\". Shape, enamel, picture and wording are yours."), style = T.Small, modifier = Modifier.padding(vertical = 6.dp))
                Btn(w.t("Учредить", "Institute"), Modifier.fillMaxWidth()) { nav.push(Overlay.Edit(0)) }
            }
            val own = h.s.own.filter { !it.archived }
            if (own.isNotEmpty()) {
                Label(w.t("Учреждённые знаки", "Instituted badges"))
                for (o in own) {
                    val b = Registry.badge(o)
                    val n = h.s.count(b.key)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(8.dp)).background(C.card).clickable { nav.push(Overlay.Detail(b.key)) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BadgeIcon(b, if (n > 0) h.s.metal(b.key) else Metal.BRONZE, 46.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(o.name, style = T.BodyB)
                            Text(if (n > 0) "${w.metal(h.s.metal(b.key))} · ${w.times(n)}" else w.t("ещё не вручался — отметь в комиссии", "not yet awarded: tick it at the commission"), style = T.Small)
                        }
                    }
                }
            }
            Label(w.t("Наградить другого", "Honour someone else"))
            Card {
                Text(w.t("Любой знак можно вручить другу, коллеге или маме — комиссия оформит наградной лист с печатью. Выбери знак:", "Any badge can go to a friend, a colleague or your mum: the commission draws up an award sheet with a stamp. Pick a badge:"), style = T.Small)
                Gap(8)
                val earned = h.s.counts.entries.sortedByDescending { it.value }.mapNotNull { h.badge(it.key) }.take(18)
                val pool = earned.ifEmpty { h.catalog.items.take(12) }
                Flow(gap = 2.dp) {
                    for (b in pool) Box(Modifier.clickable { nav.push(Overlay.Give(b.key)) }) { BadgeIcon(b, if (h.s.count(b.key) > 0) h.s.metal(b.key) else Metal.BRONZE, 44.dp) }
                }
                Text(w.t("Весь перечень — в книжке, во вкладке «Коллекция».", "The whole list is in the record, under Collection."), style = T.Small, modifier = Modifier.padding(top = 6.dp))
            }
            if (h.s.given.isNotEmpty()) {
                Label(w.t("Выданные листы", "Sheets given"))
                for (g in h.s.given.reversed()) {
                    val b = h.badge(g.key) ?: continue
                    Text("${w.date(g.at.date(h.zone))} — ${g.to}: ${w.quoted(b)}", style = T.Order, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            Gap(24)
        }
    }
}

/** Openers for a decree, in the commission's own style; tapping one starts the wording. */
private val OPENERS = listOf(
    "За беспримерное мужество, проявленное при" to "For unparalleled courage displayed in",
    "За образцовое" to "For exemplary",
    "За неоднократное" to "For repeated",
    "За личный вклад в дело" to "For a personal contribution to the cause of",
    "За стойкость в условиях" to "For fortitude in the face of",
    "За верность принципу" to "For loyalty to the principle of",
    "За самоотверженный отказ от" to "For the selfless renunciation of",
)

/** Making a badge of your own: wording, shape, two enamels, and a picture from the stamps or drawn by hand. */
@Composable
fun OwnEditor(h: Home, id: Long, nav: Nav) {
    val w = h.w
    val c = LocalContext.current.container
    val toast = LocalToast.current
    val old = h.s.ownBadge(id)
    var name by remember { mutableStateOf(old?.name ?: "") }
    var deed by remember { mutableStateOf(old?.deed ?: "") }
    var decree by remember { mutableStateOf(old?.decree ?: "") }
    var shape by remember { mutableStateOf(old?.shape ?: Shape.STAR) }
    var enamel by remember { mutableIntStateOf(old?.enamel ?: 0) }
    var band by remember { mutableIntStateOf(old?.band ?: 5) }
    var icon by remember { mutableStateOf(old?.icon ?: "star") }
    var pixels by remember { mutableStateOf(old?.pixels) }
    var ink by remember { mutableIntStateOf(1) }
    val light by rememberLight()
    val look = Look(shape, enamel, band, if (pixels == null) icon else null, pixels)

    Sheet(if (old == null) w.t("Новый знак", "A new badge") else w.t("Знак", "The badge"), { nav.pop() }, w.t("Закрыть", "Close")) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(2f)) { badge(look, Metal.GOLD, Offset(size.width / 2, size.height / 2), size.height * .42f, light) }
        Field(name, { name = it }, w.t("Название: «Укротитель сорняков»", "Title: \"Tamer of Weeds\""), style = T.Heading, max = 40)
        Gap(6)
        Field(deed, { deed = it }, w.t("Подвиг, как его отмечать: «Прополол{|а} грядку»", "The deed as you'll tick it: \"Weeded a bed\""), max = 60)
        Gap(6)
        Field(decree, { decree = it }, w.t("Указ: «За …»", "The decree: \"For …\""), style = T.Order, minLines = 2, max = 160)
        Flow(Modifier.padding(top = 6.dp)) {
            for ((r, e) in OPENERS) Chip(if (h.ru) r else e, false) { decree = (if (h.ru) r else e) + " " }
        }
        Label(w.t("Форма", "Shape"))
        Flow(gap = 4.dp) {
            for (s in Shape.entries) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (s == shape) C.gold.copy(alpha = .5f) else Color.Transparent).clickable { shape = s }.padding(2.dp)) {
                Canvas(Modifier.size(44.dp)) { badge(Look(s, enamel, band, null), Metal.GOLD, Offset(size.width / 2, size.height / 2), size.minDimension * .44f, shadow = false) }
            }
        }
        Label(w.t("Эмаль", "Enamel"))
        Swatches(enamel) { enamel = it }
        Label(w.t("Вторая эмаль", "Second enamel"))
        Swatches(band) { band = it }
        Label(w.t("Рисунок", "Picture"))
        Flow {
            Chip(w.t("Штамп", "Stamp"), pixels == null) { pixels = null }
            Chip(w.t("Нарисовать", "Draw"), pixels != null) { if (pixels == null) pixels = "0".repeat(Look.GRID * Look.GRID) }
        }
        Gap(8)
        if (pixels == null) {
            Flow(gap = 2.dp) {
                for (n in Icons.names) Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (n == icon) C.gold else Color.Transparent).clickable { icon = n }.padding(2.dp)) {
                    BadgeIcon(app.honours.domain.Badge("i", app.honours.domain.Source.OWN, null, Look(Shape.ROUND, enamel, band, n), app.honours.domain.Tx.same(""), app.honours.domain.Tx.same(""), app.honours.domain.Tx.same("")), Metal.GOLD, 40.dp)
                }
            }
        } else {
            Flow(gap = 6.dp) {
                for (d in 0..Badges.PIXEL_COLORS) {
                    val col = Badges.pixelColor(d, Badges.tone(Metal.GOLD))
                    Box(Modifier.size(32.dp).clip(CircleShape).background(col ?: C.card).border(if (d == ink) 3.dp else 1.dp, if (d == ink) C.ink else C.line, CircleShape).clickable { ink = d }, contentAlignment = Alignment.Center) {
                        if (d == 0) Text("✕", style = T.Small)
                    }
                }
            }
            Gap(8)
            PixelGrid(pixels!!, ink) { pixels = it }
            Text(w.t("Рисуй пальцем. Крестик — ластик.", "Draw with a finger. The cross is the eraser."), style = T.Small)
        }
        Gap(16)
        Btn(if (old == null) w.t("Учредить знак", "Institute the badge") else w.t("Сохранить", "Save"), Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
            val o = (old ?: Own(name = "", deed = "", decree = "", shape = shape, enamel = 0, band = 0, icon = null, pixels = null, createdAt = 0)).copy(
                name = name.trim(), deed = deed.trim(), decree = decree.trim().ifEmpty { w.t("За подвиг, достойный отдельного знака", "For a deed worthy of a badge of its own") }.trimEnd('.'),
                shape = shape, enamel = enamel, band = band, icon = if (pixels == null) icon else null, pixels = pixels,
            )
            c.write { repo.saveOwn(o) }
            toast.show(if (old == null) w.t("Знак учреждён. Он ждёт в комиссии.", "Badge instituted. It's waiting at the commission.") else w.t("Сохранено", "Saved"))
            nav.pop()
        }
        if (old != null) {
            Gap(8)
            Btn(w.t("Убрать из комиссии", "Retire from the commission"), Modifier.fillMaxWidth(), kind = Kind.GHOST) {
                c.write { repo.archive(old.id) }; nav.pop()
            }
            Text(w.t("Вручённое остаётся вручённым: знак останется на вымпеле и в книжке.", "What was awarded stays awarded: the badge stays on the pennant and in the record."), style = T.Small, modifier = Modifier.padding(top = 4.dp))
        }
        Gap(16)
    }
}

@Composable
private fun Swatches(sel: Int, onPick: (Int) -> Unit) {
    Flow(gap = 6.dp) {
        for ((i, col) in Badges.ENAMEL.withIndex()) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(col).border(if (i == sel) 3.dp else 1.dp, if (i == sel) C.ink else C.line, CircleShape).clickable { onPick(i) })
        }
    }
}

/** A 9×9 grid to paint with a finger, one colour at a time. */
@Composable
private fun PixelGrid(px: String, ink: Int, onChange: (String) -> Unit) {
    val n = Look.GRID
    var cells by remember(px) { mutableStateOf(px) }
    fun paint(p: Offset, size: Size) {
        val i = (p.x / size.width * n).toInt(); val j = (p.y / size.height * n).toInt()
        if (i !in 0 until n || j !in 0 until n) return
        val k = j * n + i
        if (cells[k].digitToInt() == ink) return
        cells = cells.substring(0, k) + ink.toString() + cells.substring(k + 1)
        onChange(cells)
    }
    var box by remember { mutableStateOf(Size.Zero) }
    Canvas(
        Modifier.fillMaxWidth(.8f).aspectRatio(1f).pointerInput(ink) { detectTapGestures { paint(it, box) } }
            .pointerInput(ink) { detectDragGestures { ch, _ -> ch.consume(); paint(ch.position, box) } },
    ) {
        box = size
        val cell = size.width / n
        drawRect(C.card)
        val tone = Badges.tone(Metal.GOLD)
        for (k in 0 until n * n) {
            val col = Badges.pixelColor(cells[k].digitToInt(), tone) ?: continue
            drawRect(col, Offset((k % n) * cell, (k / n) * cell), Size(cell, cell))
        }
        for (i in 0..n) {
            drawLine(C.line, Offset(i * cell, 0f), Offset(i * cell, size.height), 1f)
            drawLine(C.line, Offset(0f, i * cell), Offset(size.width, i * cell), 1f)
        }
        drawRect(C.ink, style = Stroke(2f))
    }
}
