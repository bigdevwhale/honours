package app.honours.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.honours.BuildConfig
import app.honours.container
import app.honours.data.Settings
import app.honours.domain.Gender
import app.honours.notify.Sitting
import app.honours.ui.Home
import app.honours.ui.components.Card
import app.honours.ui.components.Chip
import app.honours.ui.components.CommunityLinks
import app.honours.ui.components.Flow
import app.honours.ui.components.Gap
import app.honours.ui.components.Sheet
import app.honours.ui.theme.C
import app.honours.ui.theme.T

@Composable
fun SettingsSheet(h: Home, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val st = h.settings
    fun save(f: (Settings) -> Settings) = c.write { settings.update(f); Sitting.plan(ctx) }

    Sheet(w.t("Настройки", "Settings"), onClose, w.t("Закрыть", "Close")) {
        Card {
            Text(w.t("Язык", "Language"), style = T.Heading)
            Flow(Modifier.padding(top = 8.dp)) {
                Chip("Русский", h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru")) }
                Chip("English", !h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
            }
            if (h.ru) {
                Text("Как к тебе обращаться", style = T.BodyB, modifier = Modifier.padding(top = 12.dp))
                Flow(Modifier.padding(top = 6.dp)) {
                    Chip("помыл", st.gender == Gender.HE) { save { it.copy(gender = Gender.HE) } }
                    Chip("помыла", st.gender == Gender.SHE) { save { it.copy(gender = Gender.SHE) } }
                }
            }
        }
        Gap(10)
        Card {
            Text(w.t("Заседание комиссии", "The commission sits"), style = T.Heading)
            Text(w.t("Одно уведомление вечером, если за день ещё ничего не отмечено. Это не напоминание — просто приглашение.",
                "One notification in the evening if nothing has been ticked that day. Not a reminder, just an invitation."), style = T.Small)
            Flow(Modifier.padding(top = 8.dp)) {
                for (hr in listOf(19, 20, 21, 22, 23)) Chip("$hr:00", st.hour == hr) { save { it.copy(hour = hr) } }
                Chip(w.t("не звать", "never"), st.hour < 0) { save { it.copy(hour = -1) } }
            }
        }
        Gap(10)
        Card {
            Text(w.t("Звук", "Sound"), style = T.Heading)
            Flow(Modifier.padding(top = 8.dp)) {
                Chip(w.t("фанфары и печать", "fanfare and stamp"), st.sound) { save { it.copy(sound = true) } }
                Chip(w.t("тихо", "silent"), !st.sound) { save { it.copy(sound = false) } }
            }
        }
        Gap(10)
        CommunityLinks(w)
        Gap(10)
        Card {
            Text(w.t("О «Значкисте»", "About Small Honours"), style = T.Heading)
            Text(w.t("Кто сказал, что достижения должны быть большими? Здесь награждают за малое — с приказом, печатью и фанфарами. Без рекламы, без аккаунта, без интернета.",
                "Who said achievements have to be big? Here the small ones are honoured, with an order, a stamp and a fanfare. No ads, no account, no internet."), style = T.Small)
            Text(w.t("Версия ${BuildConfig.VERSION_NAME} · шрифты PT Sans и PT Mono (OFL) · исходники на GitHub", "Version ${BuildConfig.VERSION_NAME} · PT Sans and PT Mono fonts (OFL) · source on GitHub"),
                style = T.Small.copy(color = C.redDeep),
                modifier = Modifier.padding(top = 8.dp).clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bigdevwhale/honours"))) } })
        }
        Gap(20)
    }
}
