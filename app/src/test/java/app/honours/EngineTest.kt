package app.honours

import androidx.test.core.app.ApplicationProvider
import app.honours.data.HonoursDb
import app.honours.data.Repo
import app.honours.domain.Catalog
import app.honours.domain.Engine
import app.honours.domain.Metal
import app.honours.domain.Rules
import app.honours.domain.Specials
import app.honours.domain.State
import app.honours.domain.Tag
import app.honours.ui.Words
import app.honours.ui.art.Icons
import app.honours.domain.Gender
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EngineTest {
    private val catalog = File("src/main/assets/feats.tsv").inputStream().use { Catalog.read(it) }
    private val zone = ZoneId.of("Europe/Berlin")

    private fun repo(): Repo = Repo(HonoursDb.memory(ApplicationProvider.getApplicationContext()), catalog).also {
        it.zone = zone
        it.rnd = Random(1)
    }

    private fun at(d: LocalDate, h: Int) = d.atTime(h, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun catalogueIsWhole() {
        assertTrue(catalog.items.size >= 300)
        assertEquals(catalog.items.size, catalog.items.map { it.key }.toSet().size)
        for (b in catalog.items) {
            assertNotNull("icon ${b.look.icon} of ${b.key}", Icons[b.look.icon!!])
            assertTrue(b.key, b.decree.ru.startsWith("За ") && b.decree.en.startsWith("For "))
            for (s in listOf(b.deed.ru, b.name.ru, b.decree.ru)) assertEquals(b.key, s.count { it == '{' }, s.count { it == '}' })
        }
        for (b in Specials.all) assertNotNull(Icons[b.look.icon!!])
    }

    @Test fun genderMarks() {
        val b = catalog["pan_overnight"]!!
        assertEquals("Помыла сковороду, оставленную на ночь", Words(true, Gender.SHE).deed(b))
        assertEquals("Помыл сковороду, оставленную на ночь", Words(true, Gender.HE).deed(b))
        assertEquals("Помыл(а) сковороду, оставленную на ночь", Words(true, null).deed(b))
    }

    @Test fun classes() {
        assertEquals(Metal.BRONZE, Rules.metal(1))
        assertEquals(Metal.SILVER, Rules.metal(Rules.SILVER))
        assertEquals(Metal.GOLD, Rules.metal(Rules.GOLD))
    }

    @Test fun spotsStayOnTheFelt() {
        val r = Random(5)
        val pins = mutableListOf<app.honours.domain.Pin>()
        repeat(Rules.PER_PENNANT) { i ->
            val (x, y) = Engine.spot(pins, r)
            assertTrue("$x,$y", Engine.onFelt(x, y))
            pins += app.honours.domain.Pin(0, "k$i", x, y, 0)
        }
        val (cx, cy) = Engine.clamp(-50f, 900f)
        assertTrue(Engine.onFelt(cx, cy))
    }

    @Test fun seasonsKeepToTheirTime() {
        val winter = catalog.items.filter { Tag.WINTER in it.tags }
        val summer = Engine.context(at(LocalDate.of(2026, 7, 10), 21), zone)
        assertTrue(winter.isNotEmpty())
        assertTrue(winter.none { Engine.fits(it, summer) })
    }

    @Test fun agendaIsStableForADay() {
        val now = at(LocalDate.of(2026, 10, 26), 21)
        val a = Engine.agenda(State.EMPTY, catalog, now, zone, 9)
        assertEquals(12, a.size)
        assertEquals(a, Engine.agenda(State.EMPTY, catalog, now + 3_600_000, zone, 9))
        assertFalse(a == Engine.agenda(State.EMPTY, catalog, now + 86_400_000, zone, 9))
    }

    @Test fun awardsPinsAndSpecials() = runBlocking {
        val repo = repo()
        val day = LocalDate.of(2026, 10, 26)
        repo.clock = { at(day, 2) }
        val r = repo.award(listOf("pan_overnight", "bed_made"))
        val keys = r.map { it.key }
        assertTrue(keys.containsAll(listOf("pan_overnight", "bed_made", Specials.FIRST, Specials.NIGHT)))
        assertTrue(r.all { it.pin != null })
        // The same deed twice in a day counts once.
        assertTrue(repo.award(listOf("pan_overnight")).isEmpty())
        val s = repo.refresh()
        assertEquals(1, s.count("pan_overnight"))
        assertEquals(4, s.pinsOn(0).size)
    }

    @Test fun classRisesAndPennantFills() = runBlocking {
        val repo = repo()
        var day = LocalDate.of(2026, 1, 5)
        var up = false
        for (i in 1..Rules.SILVER) {
            val d = day
            repo.clock = { at(d, 21) }
            up = repo.award(listOf("bed_made")).first { it.key == "bed_made" }.up
            day = day.plusDays(1)
        }
        assertTrue(up)
        assertEquals(Metal.SILVER, repo.refresh().metal("bed_made"))
        // Enough different badges to fill the first pennant and start the second, with the standard-bearer's badge.
        val more = catalog.items.take(Rules.PER_PENNANT + 2).map { it.key }
        repo.clock = { at(day, 21) }
        repo.award(more)
        val s = repo.refresh()
        assertEquals(Rules.PER_PENNANT, s.pinsOn(0).size)
        assertTrue(s.pennant >= 1)
        assertEquals(1, s.count(Specials.BANNER))
    }

    @Test fun returnAfterSilenceAndSurvived() = runBlocking {
        val repo = repo()
        val d = LocalDate.of(2026, 3, 1)
        repo.clock = { at(d, 21) }
        assertTrue(repo.award(listOf("bed_made")).any { it.key == Specials.SEASON })
        val later = d.plusDays(40)
        repo.clock = { at(later, 21) }
        assertTrue(repo.award(listOf("bed_made")).any { it.key == Specials.RETURN })
        assertNotNull(repo.survived())
        assertEquals(null, repo.survived())
    }

    @Test fun revokeTakesTheBadgeOff() = runBlocking {
        val repo = repo()
        repo.clock = { at(LocalDate.of(2026, 5, 5), 21) }
        val r = repo.award(listOf("bed_made"))
        repo.revoke(r.first { it.key == "bed_made" }.id)
        val s = repo.refresh()
        assertEquals(0, s.count("bed_made"))
        assertTrue(s.pins.none { it.key == "bed_made" })
    }
}
