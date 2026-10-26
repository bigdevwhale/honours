package app.honours

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.honours.data.Awarded
import app.honours.data.DemoSeed
import app.honours.domain.Gender
import app.honours.domain.Specials
import app.honours.ui.Home
import app.honours.ui.Nav
import app.honours.ui.Words
import app.honours.ui.book.BookScreen
import app.honours.ui.ceremony.Ceremony
import app.honours.ui.commission.CommissionScreen
import app.honours.ui.components.LocalToast
import app.honours.ui.components.Toaster
import app.honours.ui.components.paper
import app.honours.ui.onboarding.Onboarding
import app.honours.ui.settings.SettingsSheet
import app.honours.ui.theme.HonoursTheme
import app.honours.ui.wall.BadgeSheet
import app.honours.ui.wall.GiveSheet
import app.honours.ui.wall.WallScreen
import app.honours.ui.workshop.OwnEditor
import app.honours.ui.workshop.WorkshopScreen
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo history, drawn to PNGs (run with `-Prender`), English and Russian. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<HonoursApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(c)
        val s = c.repo.refresh()
        val st = c.settings.now()
        Home(s, st, Words(lang == "ru", Gender.HE), System.currentTimeMillis(), c.catalog)
    }

    private fun nav() = Nav(mutableStateListOf()) {}

    private fun shot(name: String, content: @Composable (Home) -> Unit) {
        val h = home()
        rule.mainClock.autoAdvance = false
        rule.setContent { HonoursTheme { CompositionLocalProvider(LocalToast provides Toaster()) { Box(Modifier.fillMaxSize().paper()) { content(h) } } } }
        rule.mainClock.advanceTimeBy(if (name.startsWith("3") || name.startsWith("9")) 15_000 else 3_000)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    /** The demo's most-awarded badge, for the close-ups. */
    private fun top(h: Home) = h.s.counts.filterKeys { !it.startsWith("sp:") && !it.startsWith("own:") }.maxBy { it.value }.key

    @Test fun welcome() = shot("0-welcome") { Onboarding(it.w, it.settings.copy(onboarded = false)) }
    @Test fun wall() = shot("1-wall") { WallScreen(it, nav()) }
    @Test fun commission() = shot("2-commission") { h -> CommissionScreen(h, nav(), androidx.compose.runtime.remember { mutableStateListOf(Engine0.first(h)) }) }
    @Test fun ceremony() = shot("3-ceremony") { h ->
        val k = top(h)
        Ceremony(h, listOf(Awarded(k, h.s.awards.last().id + 1, app.honours.domain.Rules.SILVER, true, null, false))) {}
    }
    @Test fun badge() = shot("4-badge") { h -> BadgeSheet(h, top(h), nav()) }
    @Test fun book() = shot("5-book") { BookScreen(it, nav()) }
    @Test fun workshop() = shot("6-workshop") { WorkshopScreen(it, nav()) }
    @Test fun editor() = shot("7-editor") { h -> OwnEditor(h, h.s.own.first().id, nav()) }
    @Test fun give() = shot("8-give") { h -> GiveSheet(h, top(h), nav()) }
    @Test fun survived() = shot("9-survived") { h -> Ceremony(h, listOf(Awarded(Specials.SURVIVED, h.s.awards.last().id + 1, 1, false, null, false))) {} }
    @Test fun settings() = shot("x-settings") { SettingsSheet(it) {} }
}

private object Engine0 {
    fun first(h: Home) = app.honours.domain.Engine.agenda(h.s, h.catalog, h.now, h.zone, h.settings.salt).first().key
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
