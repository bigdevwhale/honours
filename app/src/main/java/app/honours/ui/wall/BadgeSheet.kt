package app.honours.ui.wall

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.honours.container
import app.honours.domain.Metal
import app.honours.domain.Rules
import app.honours.domain.Source
import app.honours.domain.date
import app.honours.ui.Home
import app.honours.ui.Nav
import app.honours.ui.Overlay
import app.honours.ui.Words
import app.honours.ui.art.Badges.badge
import app.honours.ui.art.Badges.blank
import app.honours.ui.components.Btn
import app.honours.ui.components.Card
import app.honours.ui.components.Field
import app.honours.ui.components.Gap
import app.honours.ui.components.Kind
import app.honours.ui.components.LocalToast
import app.honours.ui.components.Sheet
import app.honours.ui.components.rememberLight
import app.honours.ui.theme.C
import app.honours.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One badge up close: the metal catching the light, its order, its class and how far to the next. */
@Composable
fun BadgeSheet(h: Home, key: String, nav: Nav) {
    val w = h.w
    val c = LocalContext.current.container
    val b = h.badge(key) ?: run { nav.pop(); return }
    val n = h.s.count(key)
    val metal = h.s.metal(key)
    val light by rememberLight()

    Sheet(w.t("Знак отличия", "Badge of honour"), { nav.pop() }, w.t("Закрыть", "Close")) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.5f)) {
            val cc = Offset(size.width / 2, size.height / 2)
            val r = size.height * .4f
            if (n > 0) badge(b.look, metal, cc, r, light) else blank(b.look.shape, cc, r, C.paperDark, C.muted.copy(alpha = .5f))
        }
        Text(w.name(b), style = T.Title.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        Text(
            when {
                n == 0 -> w.t("ещё не вручался", "not yet awarded")
                n >= Rules.HONOURED -> w.t("${w.metal(metal)} · звание «Заслуженный»", "${w.metal(metal)} · Honoured title")
                else -> w.metal(metal)
            }.uppercase(),
            style = T.Label.copy(color = if (n > 0) C.red else C.muted, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        Gap(12)
        Card {
            Text("${w.decree(b)} — ${w.resolution(b, metal)}.", style = T.Order)
        }
        Gap(8)
        Card {
            Text(w.t("Подвиг", "The deed"), style = T.Label)
            Text(w.deed(b), style = T.Body)
            b.dept?.let { Text(w.ministry(it), style = T.Small, modifier = Modifier.padding(top = 4.dp)) }
            if (b.source == Source.SPECIAL) Text(w.t("Особый знак комиссии: его нельзя отметить самому, он находит сам.", "A special badge of the commission: you can't tick it, it finds you."), style = T.Small, modifier = Modifier.padding(top = 4.dp))
        }
        if (n > 0) {
            Gap(8)
            Card {
                val first = h.s.first(key)!!.at.date(h.zone)
                val last = h.s.last(key)!!.at.date(h.zone)
                Text(w.t("Вручён ${w.times(n)}", "Awarded ${w.times(n)}"), style = T.BodyB)
                Text(if (n == 1) w.dateY(first) else w.t("впервые ${w.dateY(first)}, последний раз ${w.dateY(last)}", "first on ${w.dateY(first)}, last on ${w.dateY(last)}"), style = T.Small)
                val next = when (metal) { Metal.BRONZE -> Rules.SILVER; Metal.SILVER -> Rules.GOLD; Metal.GOLD -> Rules.HONOURED }
                if (n < next) {
                    val to = when (metal) { Metal.BRONZE -> w.metal(Metal.SILVER); Metal.SILVER -> w.metal(Metal.GOLD); Metal.GOLD -> w.t("звания «Заслуженный»", "the Honoured title") }
                    Text(w.t("До $to — ещё ${w.times(next - n)}", "${next - n} more to $to"), style = T.Small)
                }
            }
        }
        Gap(14)
        Btn(w.t("Наградить другого", "Award someone else"), Modifier.fillMaxWidth(), kind = Kind.GOLD) { nav.pop(); nav.push(Overlay.Give(key)) }
        if (b.source == Source.OWN) {
            Gap(8)
            Btn(w.t("Изменить значок", "Edit the badge"), Modifier.fillMaxWidth(), kind = Kind.SOFT) { nav.pop(); nav.push(Overlay.Edit(key.removePrefix("own:").toLong())) }
        }
        val todays = h.s.on(h.today).lastOrNull { it.key == key }
        if (todays != null) {
            Gap(8)
            Btn(w.t("Отменить сегодняшний приказ", "Revoke today's order"), Modifier.fillMaxWidth(), kind = Kind.GHOST) { c.write { repo.revoke(todays.id) }; nav.pop() }
        }
        Gap(16)
    }
}

/** An award sheet for a friend: their name, your signature, a preview, and off it goes. */
@Composable
fun GiveSheet(h: Home, key: String, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val toast = LocalToast.current
    val b = h.badge(key) ?: run { nav.pop(); return }
    var to by remember { mutableStateOf("") }
    var from by remember { mutableStateOf(h.settings.signer) }
    // Someone else's gender is unknown, so the sheet shows both forms where Russian needs one.
    val sw = remember(h.ru) { Words(h.ru, null) }
    val metal = if (h.s.count(key) > 0) h.s.metal(key) else Metal.BRONZE
    val bmp = remember(to, from, key) { AwardSheet.draw(ctx, sw, b, metal, to.trim(), from.trim(), h.now.date(h.zone)) }

    Sheet(w.t("Наградной лист", "Award sheet"), { nav.pop() }, w.t("Закрыть", "Close")) {
        Text(w.t("Заметил{|а} чужой малый подвиг? Комиссия оформит лист — отправь его картинкой.", "Noticed someone else's small deed? The commission will draw up a sheet; send it as a picture."), style = T.Body)
        Gap(10)
        Field(to, { to = it }, w.t("Кого награждаем — имя", "Who is being honoured: a name"), max = 40)
        Gap(8)
        Field(from, { from = it }, w.t("Подпись председателя — твоё имя", "The chairman's signature: your name"), max = 30)
        Gap(12)
        Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(AwardSheet.W / AwardSheet.H.toFloat()))
        Gap(12)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Btn(w.t("Отправить", "Send"), Modifier.weight(1f), enabled = to.isNotBlank()) {
                AwardSheet.share(ctx, bmp, w.t("Наградной лист", "Award sheet"))
                c.write {
                    settings.update { it.copy(signer = from.trim()) }
                    val special = repo.given(key, to)
                    if (special != null) withContext(Dispatchers.Main) { nav.pop(); nav.push(Overlay.Ceremony(listOf(special))) }
                }
                toast.show(w.t("Лист оформлен", "The sheet is drawn up"))
            }
        }
        Gap(16)
    }
}
