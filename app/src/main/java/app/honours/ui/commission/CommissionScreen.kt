package app.honours.ui.commission

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.honours.container
import app.honours.domain.Badge
import app.honours.domain.Dept
import app.honours.domain.Engine
import app.honours.domain.Registry
import app.honours.domain.date
import app.honours.sound.Snd
import app.honours.ui.Home
import app.honours.ui.Nav
import app.honours.ui.Overlay
import app.honours.ui.components.BadgeIcon
import app.honours.ui.components.Btn
import app.honours.ui.components.Card
import app.honours.ui.components.Chip
import app.honours.ui.components.Field
import app.honours.ui.components.Flow
import app.honours.ui.components.Gap
import app.honours.ui.components.Kind
import app.honours.ui.components.Label
import app.honours.ui.components.Sheet
import app.honours.ui.components.TopBar
import app.honours.ui.theme.C
import app.honours.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The evening sitting: today's agenda of small deeds to tick, your own badges, the full list, and for a day
 * with nothing in it, the Order of the Day Survived. Ticked deeds go to the ceremony together.
 */
@Composable
fun CommissionScreen(h: Home, nav: Nav, picked: SnapshotStateList<String>) {
    val w = h.w
    val c = LocalContext.current.container
    val today = h.s.on(h.today)
    val todayKeys = today.map { it.key }.toSet()
    val agenda = remember(h.today, h.s.awards.size, h.settings.salt) { Engine.agenda(h.s, h.catalog, h.now, h.zone, h.settings.salt) }
    val own = h.s.own.filter { !it.archived && it.key !in todayKeys }.map(Registry::badge)
    val extra = picked.filter { k -> agenda.none { it.key == k } && own.none { it.key == k } }.mapNotNull { h.badge(it) }
    var busy by remember { mutableStateOf(false) }
    fun toggle(k: String) { if (k in picked) picked.remove(k) else picked.add(k); c.sfx.play(Snd.TICK, .5f) }

    Column(Modifier.fillMaxSize()) {
        val d = h.now.date(h.zone)
        TopBar(w.t("Наградная комиссия", "Awards Commission"), w.t("Заседание · ${w.weekday(d)}, ${w.date(d)}", "In session · ${w.weekday(d)}, ${w.date(d)}"))
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp)) {
            if (today.isNotEmpty()) {
                item {
                    Label(w.t("Уже вручено сегодня", "Already awarded today"))
                    Flow {
                        for (a in today) h.badge(a.key)?.let { b -> Box(Modifier.clickable { nav.push(Overlay.Detail(b.key)) }) { BadgeIcon(b, h.s.metal(b.key), 46.dp) } }
                    }
                }
            }
            item { Label(w.t("Что было сегодня?", "What happened today?")) }
            items(agenda, key = { it.key }) { b -> DeedRow(h, b, b.key in picked) { toggle(b.key) } }
            if (extra.isNotEmpty()) {
                item { Label(w.t("Из полного перечня", "From the full list")) }
                items(extra, key = { "x" + it.key }) { b -> DeedRow(h, b, true) { toggle(b.key) } }
            }
            if (own.isNotEmpty()) {
                item { Label(w.t("Свои знаки", "Your own badges")) }
                items(own, key = { it.key }) { b -> DeedRow(h, b, b.key in picked) { toggle(b.key) } }
            }
            item {
                Gap(6)
                Btn(w.t("Весь перечень подвигов · ${h.catalog.items.size}", "The full list of deeds · ${h.catalog.items.size}"), Modifier.fillMaxWidth(), kind = Kind.SOFT) { nav.push(Overlay.AllDeeds) }
                Gap(14)
                if (todayKeys.none { it == app.honours.domain.Specials.SURVIVED }) {
                    Card(bg = C.velvet) {
                        Text(if (today.isEmpty()) w.t("Ничего не было? Тяжёлый день?", "Nothing happened? A hard day?") else w.t("И всё-таки тяжёлый день?", "A hard day all the same?"), style = T.Heading.copy(color = C.gold))
                        Text(w.t("Комиссия знает: иногда подвиг — просто дожить до вечера. За это полагается орден.", "The commission knows: sometimes the deed is simply getting to the evening. There's an order for that."),
                            style = T.Small.copy(color = C.card.copy(alpha = .8f)), modifier = Modifier.padding(vertical = 6.dp))
                        Btn(w.t("Орден Пережитого Дня", "Order of the Day Survived"), Modifier.fillMaxWidth(), kind = Kind.GOLD, enabled = !busy) {
                            busy = true
                            c.write {
                                val r = repo.survived()
                                withContext(Dispatchers.Main) { busy = false; if (r != null) nav.push(Overlay.Ceremony(listOf(r))) }
                            }
                        }
                    }
                }
                Gap(20)
            }
        }
        Box(Modifier.fillMaxWidth().background(C.paper).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Btn(
                if (picked.isEmpty()) w.t("Отметь, что было", "Tick what happened") else w.t("Представить к награде · ${picked.size}", "Put forward for awards · ${picked.size}"),
                Modifier.fillMaxWidth(), enabled = picked.isNotEmpty() && !busy,
            ) {
                busy = true
                val keys = picked.toList()
                c.write {
                    val r = repo.award(keys)
                    withContext(Dispatchers.Main) { busy = false; picked.clear(); if (r.isNotEmpty()) nav.push(Overlay.Ceremony(r)) }
                }
            }
        }
    }
}

/** A deed on the agenda: its badge (or the blank of one not yet earned), the deed, and a box to tick. */
@Composable
fun DeedRow(h: Home, b: Badge, on: Boolean, onToggle: () -> Unit) {
    val w = h.w
    val n = h.s.count(b.key)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(8.dp)).background(if (on) C.card else C.card.copy(alpha = .55f))
            .border(1.dp, if (on) C.red.copy(alpha = .6f) else C.line, RoundedCornerShape(8.dp)).clickable(onClick = onToggle).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BadgeIcon(b, if (n > 0) h.s.metal(b.key) else null, 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(w.deed(b), style = T.Body)
            Text(
                if (n > 0) "${w.quoted(b)} · ${w.metal(h.s.metal(b.key))} · ${w.times(n)}"
                else b.dept?.let { w.t("новый знак · ${w.dept(it)}", "a new badge · ${w.dept(it)}") } ?: w.t("свой знак", "your own badge"),
                style = T.Small,
            )
        }
        Spacer(Modifier.width(8.dp))
        TickBox(on)
    }
}

/** A square box ticked in red ink, a little carelessly, as on a paper form. */
@Composable
fun TickBox(on: Boolean) {
    Canvas(Modifier.size(26.dp)) {
        drawRect(C.ink.copy(alpha = .6f), Offset(2.dp.toPx(), 2.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        if (on) {
            val s = size.width
            drawLine(C.red, Offset(s * .18f, s * .5f), Offset(s * .42f, s * .78f), 3.dp.toPx(), StrokeCap.Round)
            drawLine(C.red, Offset(s * .42f, s * .78f), Offset(s * .92f, s * .05f), 3.dp.toPx(), StrokeCap.Round)
        }
    }
}

/** Every deed in the catalogue, by ministry, with a search; ticks go back to today's agenda. */
@Composable
fun AllDeedsSheet(h: Home, nav: Nav, picked: SnapshotStateList<String>) {
    val w = h.w
    val c = LocalContext.current.container
    var q by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf<Dept?>(null) }
    val done = h.s.on(h.today).map { it.key }.toSet()
    val list = h.catalog.items.filter { b ->
        b.key !in done && (dept == null || b.dept == dept) &&
            (q.isBlank() || w.deed(b).contains(q.trim(), true) || w.name(b).contains(q.trim(), true))
    }
    Sheet(w.t("Перечень подвигов", "The list of deeds"), { nav.pop() }, w.t("Готово · ${picked.size}", "Done · ${picked.size}"), scroll = false) {
        Field(q, { q = it }, w.t("Поиск: сковорода, бабушка, лужа…", "Search: pan, grandma, puddle…"), max = 40)
        Gap(8)
        Flow {
            Chip(w.t("Все", "All"), dept == null) { dept = null }
            for (d in Dept.entries) Chip(w.dept(d), dept == d) { dept = if (dept == d) null else d }
        }
        Gap(6)
        LazyColumn(Modifier.weight(1f).navigationBarsPadding()) {
            items(list, key = { it.key }) { b -> DeedRow(h, b, b.key in picked) { if (b.key in picked) picked.remove(b.key) else picked.add(b.key); c.sfx.play(Snd.TICK, .5f) } }
            if (list.isEmpty()) item { Text(w.t("Такого подвига в перечне нет. Его можно учредить в мастерской.", "No such deed in the list. You can institute it in the workshop."), style = T.Small, modifier = Modifier.padding(12.dp)) }
        }
    }
}
