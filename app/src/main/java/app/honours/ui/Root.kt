package app.honours.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.honours.R
import app.honours.container
import app.honours.data.Awarded
import app.honours.data.Settings
import app.honours.domain.Badge
import app.honours.domain.Catalog
import app.honours.domain.Registry
import app.honours.domain.State
import app.honours.domain.day
import app.honours.notify.Sitting
import app.honours.ru
import app.honours.ui.book.BookScreen
import app.honours.ui.ceremony.Ceremony
import app.honours.ui.commission.AllDeedsSheet
import app.honours.ui.commission.CommissionScreen
import app.honours.ui.components.LocalToast
import app.honours.ui.components.ToastHost
import app.honours.ui.components.Toaster
import app.honours.ui.components.paper
import app.honours.ui.onboarding.Onboarding
import app.honours.ui.settings.SettingsSheet
import app.honours.ui.wall.BadgeSheet
import app.honours.ui.wall.GiveSheet
import app.honours.ui.wall.WallScreen
import app.honours.ui.workshop.OwnEditor
import app.honours.ui.workshop.WorkshopScreen
import app.honours.ui.theme.C
import app.honours.ui.theme.T
import java.time.ZoneId
import kotlinx.coroutines.delay

/** Everything a screen reads, at one moment. */
class Home(val s: State, val settings: Settings, val w: Words, val now: Long, val catalog: Catalog) {
    val ru get() = w.ru
    val zone: ZoneId = ZoneId.systemDefault()
    val today: Long get() = now.day(zone)
    val reg = Registry(catalog, s.own)
    fun badge(key: String): Badge? = reg[key]
}

enum class Tab { WALL, COMMISSION, BOOK, WORKSHOP }

sealed interface Overlay {
    data class Ceremony(val awards: List<Awarded>) : Overlay
    data class Detail(val key: String) : Overlay
    data class Give(val key: String) : Overlay
    data class Edit(val id: Long) : Overlay
    data object AllDeeds : Overlay
    data object Settings : Overlay
}

/** How screens open sheets and switch tabs. */
class Nav(val overlays: SnapshotStateList<Overlay>, val tab: (Tab) -> Unit) {
    fun push(o: Overlay) = overlays.add(o)
    fun pop() { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }
}

@Composable
fun HonoursRoot(settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val state by c.repo.state.collectAsStateWithLifecycle()
    val words = remember(ctx.ru, settings.gender) { Words(ctx.ru, settings.gender) }
    val toaster = remember { Toaster() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settings.sound) { c.sfx.on = settings.sound }
    if (!settings.onboarded) {
        CompositionLocalProvider(LocalToast provides toaster) { Box(Modifier.fillMaxSize()) { Onboarding(words, settings); ToastHost(toaster) } }
        return
    }
    val life = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        life.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            c.repo.refresh()
            Sitting.plan(ctx)
            while (true) { now = System.currentTimeMillis(); delay(30_000) }
        }
    }
    val s = state ?: run { Box(Modifier.fillMaxSize().paper()); return }
    val home = Home(s, settings, words, now, c.catalog)

    // The evening notification opens straight onto the commission.
    val fromSitting = remember { (ctx as? Activity)?.intent?.getBooleanExtra("commission", false) == true }
    var tab by remember { mutableStateOf(if (fromSitting) Tab.COMMISSION else Tab.WALL) }
    val overlays = remember { mutableStateListOf<Overlay>() }
    val nav = remember { Nav(overlays) { tab = it } }
    /** What's ticked on today's agenda, kept while the full list is open on top. */
    val picked = remember { mutableStateListOf<String>() }
    BackHandler(enabled = overlays.isEmpty() && tab != Tab.WALL) { tab = Tab.WALL }

    CompositionLocalProvider(LocalToast provides toaster) {
        Box(Modifier.fillMaxSize().paper()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        Tab.WALL -> WallScreen(home, nav)
                        Tab.COMMISSION -> CommissionScreen(home, nav, picked)
                        Tab.BOOK -> BookScreen(home, nav)
                        Tab.WORKSHOP -> WorkshopScreen(home, nav)
                    }
                }
                TabBar(tab) { tab = it }
            }
            when (val o = overlays.lastOrNull()) {
                is Overlay.Ceremony -> Ceremony(home, o.awards, onDone = { nav.pop(); tab = Tab.WALL })
                is Overlay.Detail -> BadgeSheet(home, o.key, nav)
                is Overlay.Give -> GiveSheet(home, o.key, nav)
                is Overlay.Edit -> OwnEditor(home, o.id, nav)
                Overlay.AllDeeds -> AllDeedsSheet(home, nav, picked)
                Overlay.Settings -> SettingsSheet(home, onClose = { nav.pop() })
                null -> Unit
            }
            ToastHost(toaster)
        }
    }
}

@Composable
private fun TabBar(tab: Tab, onTab: (Tab) -> Unit) {
    val items = listOf(Tab.WALL to R.string.tab_wall, Tab.COMMISSION to R.string.tab_commission, Tab.BOOK to R.string.tab_book, Tab.WORKSHOP to R.string.tab_workshop)
    Column(Modifier.fillMaxWidth().background(C.card)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(C.red))
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            for ((t, label) in items) {
                val sel = t == tab
                Box(Modifier.weight(1f).clickable { onTab(t) }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(label).uppercase(), style = T.Button.copy(color = if (sel) C.red else C.muted, fontSize = T.Label.fontSize * 1.15f))
                }
            }
        }
    }
}
