package app.honours.ui.wall

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.honours.domain.Rules
import app.honours.ui.Home
import app.honours.ui.Nav
import app.honours.ui.Overlay
import app.honours.ui.Tab
import app.honours.ui.art.Pennants.wallpaper
import app.honours.ui.components.Btn
import app.honours.ui.components.Kind
import app.honours.ui.components.TopBar
import app.honours.ui.components.rememberLight
import app.honours.ui.theme.C
import app.honours.ui.theme.T
import kotlinx.coroutines.launch

/** The wall: the current pennant, the earlier ones beside it, and the way to the commission. */
@Composable
fun WallScreen(h: Home, nav: Nav) {
    val w = h.w
    val count = h.s.pennant + 1
    val pager = rememberPagerState(initialPage = h.s.pennant) { count }
    // A new pennant was just hung: turn to it.
    LaunchedEffect(h.s.pennant) { pager.animateScrollToPage(h.s.pennant) }
    val scope = rememberCoroutineScope()
    val light by rememberLight()
    val page = pager.currentPage
    val pins = h.s.pinsOn(page).size

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) { wallpaper() }
        Column(Modifier.fillMaxSize()) {
            TopBar(
                if (page == h.s.pennant) w.t("Вымпел № ${page + 1}", "Pennant No. ${page + 1}") else w.t("Вымпел № ${page + 1} · на стене", "Pennant No. ${page + 1} · on the wall"),
                if (h.s.awards.isEmpty()) w.t("пока пустой — первый значок впереди", "empty for now; the first badge is ahead")
                else w.t("${w.n(pins, "значок", "значка", "значков", "", "")} из ${Rules.PER_PENNANT}", "$pins of ${Rules.PER_PENNANT} badges"),
            ) {
                Text("⚙", style = T.Title.copy(color = C.muted), modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { nav.push(Overlay.Settings) }.padding(horizontal = 10.dp))
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { i ->
                PennantView(h, i, light, Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) { nav.push(Overlay.Detail(it)) }
            }
            if (count > 1) Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                for (i in 0 until count) {
                    Text(if (i == page) "★" else "☆", style = T.Body.copy(color = if (i == page) C.red else C.muted),
                        modifier = Modifier.clickable { scope.launch { pager.animateScrollToPage(i) } }.padding(horizontal = 5.dp))
                }
            }
            val today = h.s.on(h.today)
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (today.isEmpty()) {
                    Btn(w.t("Комиссия заседает", "The commission is sitting"), Modifier.fillMaxWidth()) { nav.tab(Tab.COMMISSION) }
                } else {
                    Btn(w.t("Сегодня: ${w.n(today.size, "награда", "награды", "наград", "", "")}", "Today: ${today.size} award${if (today.size == 1) "" else "s"}"), Modifier.fillMaxWidth(), kind = Kind.SOFT) { nav.tab(Tab.COMMISSION) }
                }
                Text(w.t("Нажми на значок — откроется приказ. Подержи — можно переколоть.", "Tap a badge to read its order. Hold it to move it."), style = T.Small, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
