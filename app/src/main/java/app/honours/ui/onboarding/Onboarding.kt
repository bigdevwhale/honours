package app.honours.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.honours.AppContainer
import app.honours.container
import app.honours.data.Settings
import app.honours.domain.Gender
import app.honours.domain.Look
import app.honours.domain.Metal
import app.honours.domain.Shape
import app.honours.notify.Sitting
import app.honours.ui.Words
import app.honours.ui.art.Badges.badge
import app.honours.ui.components.Btn
import app.honours.ui.components.Chip
import app.honours.ui.components.Flow
import app.honours.ui.components.Gap
import app.honours.ui.components.Kind
import app.honours.ui.components.LocalToast
import app.honours.ui.components.paper
import app.honours.ui.components.rememberLight
import app.honours.ui.theme.T

/** Three pages: the idea, how the commission works, and how to be addressed and invited. */
@Composable
fun Onboarding(w: Words, settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val toast = LocalToast.current
    var page by rememberSaveable { mutableIntStateOf(0) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { finish(c, ctx) }
    val light by rememberLight()

    Column(Modifier.fillMaxSize().paper().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 18.dp)) {
        when (page) {
            0 -> {
                Canvas(Modifier.fillMaxWidth().aspectRatio(1.6f)) {
                    val h = size.height
                    badge(Look(Shape.MEDAL, 2, 5, "kettle"), Metal.SILVER, Offset(size.width / 2 - h * .62f, h * .58f), h * .25f, light, tilt = -8f)
                    badge(Look(Shape.STAR, 0, 6, "pan"), Metal.GOLD, Offset(size.width / 2, h * .46f), h * .38f, light)
                    badge(Look(Shape.ROUND, 1, 5, "drop"), Metal.BRONZE, Offset(size.width / 2 + h * .62f, h * .6f), h * .24f, light, tilt = 7f)
                }
                Gap(16)
                Text(w.t("Кто сказал, что подвиги должны быть большими?", "Who said great deeds have to be big?"), style = T.Title)
                Gap(8)
                Text(
                    w.t("Помыл{|а} сковороду, которая стояла с ночи. Позвонил{|а} бабушке первым. Лёг{|ла} до полуночи. За это полагается знак — эмалевый, с приказом, печатью и фанфарами.",
                        "Washed the pan left overnight. Called grandma first. Went to bed before midnight. That deserves a badge: enamel, with an order, a stamp and a fanfare."),
                    style = T.Body,
                )
            }
            1 -> {
                Text(w.t("Как работает комиссия", "How the commission works"), style = T.Title)
                Gap(12)
                for ((look, line) in listOf(
                    Look(Shape.SHIELD, 3, 6, "moon") to w.t("Ничего не планируешь. Вечером комиссия спрашивает: что сегодня было? Отмечаешь — и значки находят тебя сами.",
                        "You plan nothing. In the evening the commission asks: what happened today? You tick it, and the badges find you."),
                    Look(Shape.ROUND, 0, 5, "star") to w.t("Каждое вручение — церемония: приказ, печать, фанфары. Наклони телефон — эмаль поймает свет.",
                        "Every award is a ceremony: an order, a stamp, a fanfare. Tilt the phone and the enamel catches the light."),
                    Look(Shape.GEAR, 6, 3, "clock") to w.t("Повторил{|а} подвиг — знак растёт: бронза, серебро, золото, а потом и звание «Заслуженный».",
                        "Do it again and the badge grows: bronze, silver, gold, then the Honoured title."),
                    Look(Shape.FLAG, 0, 6, "house") to w.t("Значки прикалываются на войлочный вымпел. Заполнен — вешается новый, а старые остаются на стене.",
                        "Badges are pinned to a felt pennant. When it's full a new one goes up, and the old ones stay on the wall."),
                    Look(Shape.ORDER, 7, 6, "candle") to w.t("Тяжёлый день, и отметить нечего? Есть Орден Пережитого Дня.",
                        "A hard day with nothing to tick? There is the Order of the Day Survived."),
                )) {
                    Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(42.dp)) { badge(look, Metal.GOLD, Offset(size.width / 2, size.height / 2), size.width * .44f, light) }
                        Spacer(Modifier.size(12.dp))
                        Text(line, style = T.Body)
                    }
                }
                Text(w.t("Ещё есть свои знаки в мастерской и наградные листы для друзей.", "There are also badges of your own in the workshop, and award sheets for friends."), style = T.Small)
            }
            else -> {
                Text(w.t("Почти всё", "Almost there"), style = T.Title)
                Gap(12)
                if (w.ru) {
                    Text("Как к тебе обращаться в приказах?", style = T.BodyB)
                    Flow(Modifier.padding(top = 8.dp)) {
                        Chip("помыл", settings.gender == Gender.HE) { c.write { this.settings.update { it.copy(gender = Gender.HE) } } }
                        Chip("помыла", settings.gender == Gender.SHE) { c.write { this.settings.update { it.copy(gender = Gender.SHE) } } }
                    }
                    Gap(16)
                }
                Text(w.t("Когда заседать комиссии?", "When should the commission sit?"), style = T.BodyB)
                Text(w.t("Одно тихое уведомление вечером — только если за день ещё ничего не отмечено.", "One quiet notification in the evening, and only if nothing has been ticked that day."), style = T.Small)
                Flow(Modifier.padding(top = 8.dp)) {
                    for (hr in listOf(19, 20, 21, 22, 23)) Chip("$hr:00", settings.hour == hr) { c.write { this.settings.update { it.copy(hour = hr) } } }
                    Chip(w.t("не звать", "never"), settings.hour < 0) { c.write { this.settings.update { it.copy(hour = -1) } } }
                }
            }
        }
        Gap(24)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (page > 0) Btn(w.t("Назад", "Back"), Modifier.weight(1f), kind = Kind.GHOST) { page-- }
            Btn(if (page < 2) w.t("Дальше", "Next") else w.t("Открыть заседание", "Open the sitting"), Modifier.weight(1.6f)) {
                when {
                    page < 2 -> page++
                    w.ru && settings.gender == null -> toast.show("Выбери, как к тебе обращаться")
                    Build.VERSION.SDK_INT >= 33 && settings.hour >= 0 -> ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else -> finish(c, ctx)
                }
            }
        }
    }
}

private fun finish(c: AppContainer, ctx: android.content.Context) = c.write {
    settings.update { it.copy(onboarded = true) }
    Sitting.plan(ctx)
}
