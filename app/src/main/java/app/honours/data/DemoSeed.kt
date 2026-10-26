package app.honours.data

import app.honours.AppContainer
import app.honours.domain.Gender
import app.honours.domain.Own
import app.honours.domain.Shape
import app.honours.localized
import app.honours.ru
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * The demo build's past: about two and a half months of sittings played through the real commission with a
 * wound-back clock, so the pennants, the record and the classes look lived in. Today is left untouched.
 */
object DemoSeed {
    /** Deeds this person does often, so their badges reach silver and gold. */
    private val HABITS = listOf("water_before_lunch", "bed_made", "called_grandma", "read_ten_pages", "lay_no_phone", "plants_watered", "evening_walk", "pan_overnight")

    /** A cat's face drawn on the 9×9 grid: 1 metal, 7 black. */
    private const val CAT =
        "100000001" +
        "110000011" +
        "111111111" +
        "117111711" +
        "111111111" +
        "111171111" +
        "111777111" +
        "011111110" +
        "001111100"

    suspend fun seedIfEmpty(c: AppContainer) {
        val repo = c.repo
        if (repo.refresh().awards.isNotEmpty()) return
        val ru = c.app.localized().ru
        c.settings.update { it.copy(onboarded = true, gender = Gender.HE, signer = if (ru) "Марат" else "Mark", salt = 7) }
        val zone = ZoneId.systemDefault()
        val rnd = Random(42)
        repo.rnd = rnd
        repo.zone = zone

        val today = LocalDate.now(zone)
        val start = today.minusDays(76)
        fun at(d: LocalDate, h: Int, m: Int) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

        repo.clock = { at(start, 12, 0) }
        val cat = repo.saveOwn(Own(
            name = if (ru) "Будильник с усами" else "Whiskered Alarm Clock", deed = if (ru) "Покормил кота, не дожидаясь трёх напоминаний" else "Fed the cat before the third reminder",
            decree = if (ru) "За своевременное снабжение хвостатого начальства без поднятия тревоги" else "For timely supplies to the whiskered management, with no alarm raised",
            shape = Shape.ROUND, enamel = 2, band = 5, icon = null, pixels = CAT, createdAt = 0,
        ))
        val sill = repo.saveOwn(Own(
            name = if (ru) "Знак «Чистый подоконник»" else "The Clear Windowsill", deed = if (ru) "Разобрал подоконник" else "Cleared the windowsill",
            decree = if (ru) "За освобождение подоконника от залежей, копившихся с прошлой весны" else "For liberating the windowsill from deposits dating back to last spring",
            shape = Shape.SHIELD, enamel = 6, band = 3, icon = "window", pixels = null, createdAt = 0,
        ))

        val items = c.catalog.items
        var d = start
        while (d.isBefore(today)) {
            val roll = rnd.nextFloat()
            if (roll < .12f) { d = d.plusDays(1); continue }
            if (roll < .16f) {
                repo.clock = { at(d, 22, 40) }
                repo.survived()
                d = d.plusDays(1); continue
            }
            val keys = mutableListOf<String>()
            for (k in HABITS) if (rnd.nextFloat() < .32f) keys += k
            repeat(rnd.nextInt(0, 3)) { keys += items[rnd.nextInt(items.size)].key }
            if (rnd.nextFloat() < .12f) keys += "own:$cat"
            if (rnd.nextFloat() < .05f) keys += "own:$sill"
            if (keys.isEmpty()) keys += HABITS[rnd.nextInt(HABITS.size)]
            val hour = if (rnd.nextFloat() < .06f) 1 else 20 + rnd.nextInt(3)
            val day = d
            repo.clock = { at(day, hour, rnd.nextInt(60)) }
            repo.award(keys.take(6))
            d = d.plusDays(1)
        }
        repo.clock = { at(today.minusDays(9), 19, 10) }
        repo.given("sincere_compliment", if (ru) "Лена" else "Lena")
        repo.clock = System::currentTimeMillis
        repo.rnd = Random.Default
        repo.refresh()
    }
}
