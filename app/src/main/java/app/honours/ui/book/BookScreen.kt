package app.honours.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.honours.domain.Dept
import app.honours.domain.Engine
import app.honours.domain.Registry
import app.honours.domain.Specials
import app.honours.domain.date
import app.honours.ui.Home
import app.honours.ui.Nav
import app.honours.ui.Overlay
import app.honours.ui.components.BadgeIcon
import app.honours.ui.components.Card
import app.honours.ui.components.Chip
import app.honours.ui.components.Flow
import app.honours.ui.components.Gap
import app.honours.ui.components.Label
import app.honours.ui.components.LocalToast
import app.honours.ui.components.TopBar
import app.honours.ui.theme.C
import app.honours.ui.theme.T
import java.time.LocalDate

private enum class Page { LOG, COLLECTION, REPORT }

/** The award record: every order by day, the whole collection to fill, and the commission's yearly report. */
@Composable
fun BookScreen(h: Home, nav: Nav) {
    val w = h.w
    var page by rememberSaveable { mutableStateOf(Page.LOG) }
    val total = h.s.awards.size
    Column(Modifier.fillMaxSize()) {
        TopBar(w.t("Наградная книжка", "Record of Awards"), w.t("${w.n(total, "награда", "награды", "наград", "", "")} · ${w.n(h.s.counts.size, "знак", "знака", "знаков", "", "")}", "$total awards · ${h.s.counts.size} badges"))
        Flow(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            Chip(w.t("Записи", "Entries"), page == Page.LOG) { page = Page.LOG }
            Chip(w.t("Коллекция", "Collection"), page == Page.COLLECTION) { page = Page.COLLECTION }
            Chip(w.t("Отчёт", "Report"), page == Page.REPORT) { page = Page.REPORT }
        }
        when (page) {
            Page.LOG -> Log(h, nav)
            Page.COLLECTION -> Collection(h, nav)
            Page.REPORT -> Report(h)
        }
    }
}

@Composable
private fun Log(h: Home, nav: Nav) {
    val w = h.w
    val days = h.s.awards.groupBy { it.day }.toSortedMap(compareByDescending { it })
    if (days.isEmpty()) {
        Text(w.t("Пока ни одной записи. Первая появится после заседания комиссии.", "No entries yet. The first will appear after the commission sits."), style = T.Body, modifier = Modifier.padding(18.dp))
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        for ((day, list) in days) {
            item(key = "d$day") {
                val d = LocalDate.ofEpochDay(day)
                Label("${w.date(d)} ${d.year} · ${w.weekday(d)}")
            }
            items(list.reversed(), key = { "a" + it.id }) { a ->
                val b = h.badge(a.key) ?: return@items
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp)).background(C.card.copy(alpha = .7f)).clickable { nav.push(Overlay.Detail(a.key)) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BadgeIcon(b, h.s.metal(a.key), 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(w.name(b), style = T.BodyB)
                        Text(w.deed(b), style = T.Small)
                    }
                    Text("№ ${a.id}", style = T.Small)
                }
            }
        }
        item { Gap(20) }
    }
}

@Composable
private fun Collection(h: Home, nav: Nav) {
    val w = h.w
    val toast = LocalToast.current
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        for (d in Dept.entries) {
            val list = h.catalog.items.filter { it.dept == d }
            val got = list.count { h.s.count(it.key) > 0 }
            item(key = d.name) {
                Label("${w.ministry(d)} · $got/${list.size}")
                Flow(gap = 2.dp) {
                    // Earned first, so the shelf fills from the left.
                    for (b in list.sortedByDescending { h.s.count(it.key) }) {
                        val n = h.s.count(b.key)
                        Box(Modifier.clickable { nav.push(Overlay.Detail(b.key)) }) { BadgeIcon(b, if (n > 0) h.s.metal(b.key) else null, 42.dp) }
                    }
                }
            }
        }
        item(key = "sp") {
            val got = Specials.all.count { h.s.count(it.key) > 0 }
            Label(w.t("Особые знаки комиссии · $got/${Specials.all.size}", "Special badges of the commission · $got/${Specials.all.size}"))
            Flow(gap = 2.dp) {
                for (b in Specials.all) {
                    val n = h.s.count(b.key)
                    Box(Modifier.clickable { if (n > 0) nav.push(Overlay.Detail(b.key)) else toast.show(w.t("Тайна комиссии. Этот знак находит сам.", "A secret of the commission. This badge finds you.")) }) {
                        BadgeIcon(b, if (n > 0) h.s.metal(b.key) else null, 42.dp)
                    }
                }
            }
        }
        val own = h.s.own.map(Registry::badge)
        if (own.isNotEmpty()) item(key = "own") {
            Label(w.t("Свои знаки", "Your own badges"))
            Flow(gap = 2.dp) {
                for (b in own) Box(Modifier.clickable { nav.push(Overlay.Detail(b.key)) }) { BadgeIcon(b, if (h.s.count(b.key) > 0) h.s.metal(b.key) else null, 42.dp) }
            }
        }
        item { Gap(24) }
    }
}

@Composable
private fun Report(h: Home) {
    val w = h.w
    val years = h.s.awards.map { it.at.date(h.zone).year }.toSortedSet(compareByDescending { it }).ifEmpty { sortedSetOf(h.now.date(h.zone).year) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        for (y in years) item(key = y) {
            val r = Engine.year(h.s, y, h.zone, h.catalog)
            Gap(10)
            Card {
                Text(w.t("Отчёт наградной комиссии за $y год", "Report of the Awards Commission for $y"), style = T.Heading)
                Gap(6)
                @Composable fun line(a: String, b: String) = Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(a, style = T.Order, modifier = Modifier.weight(1f)); Text(b, style = T.Order.copy(color = C.red))
                }
                line(w.t("Вручено наград", "Awards made"), "${r.awards}")
                line(w.t("Дней с подвигами", "Days with deeds"), "${r.days}")
                line(w.t("Различных знаков", "Different badges"), "${r.badges}")
                line(w.t("Орденов Пережитого Дня", "Orders of the Day Survived"), "${r.survived}")
                line(w.t("Наградных листов другим", "Award sheets to others"), "${h.s.given.count { it.at.date(h.zone).year == y }}")
                if (r.top.isNotEmpty()) {
                    Text(w.t("Чаще всего отмечались:", "Most often honoured:"), style = T.Small, modifier = Modifier.padding(top = 8.dp))
                    for ((k, n) in r.top) h.badge(k)?.let { b ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            BadgeIcon(b, h.s.metal(k), 34.dp); Spacer(Modifier.width(8.dp))
                            Text("${w.name(b)} — ${w.times(n)}", style = T.Body)
                        }
                    }
                }
                if (r.depts.isNotEmpty()) {
                    Text(w.t("По ведомствам:", "By ministry:"), style = T.Small, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    val max = r.depts.values.max().toFloat()
                    for (d in Dept.entries) {
                        val n = r.depts[d] ?: 0
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(w.dept(d), style = T.Small, modifier = Modifier.width(120.dp))
                            Box(Modifier.weight(1f).height(10.dp)) {
                                Box(Modifier.fillMaxWidth((n / max).coerceAtLeast(.01f)).height(10.dp).clip(RoundedCornerShape(2.dp)).background(app.honours.ui.art.Badges.ENAMEL[d.enamel].let { if (d == Dept.REST) C.muted else it }))
                            }
                            Text(" $n", style = T.Small, modifier = Modifier.width(36.dp))
                        }
                    }
                }
                Text(
                    w.t("Комиссия отмечает: ни один из этих подвигов не был мелочью.", "The commission notes: none of these deeds was a trifle."),
                    style = T.Order.copy(color = C.muted), modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
        item { Gap(24) }
    }
}
