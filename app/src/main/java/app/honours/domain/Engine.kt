package app.honours.domain

import java.time.DayOfWeek
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

/** The commission's rules, kept apart from storage so they can be tested and replayed. */
object Engine {
    private val TIME = setOf(Tag.MORNING, Tag.EVENING, Tag.NIGHT)
    private val WEEK = setOf(Tag.WEEKEND, Tag.WEEKDAY)
    private val SEASON = setOf(Tag.WINTER, Tag.SPRING, Tag.SUMMER, Tag.AUTUMN)

    /** What the moment is like: part of the day, of the week, of the year. */
    fun context(now: Long, zone: ZoneId): Set<Tag> {
        val d = now.date(zone)
        val h = now.hour(zone)
        return buildSet {
            when (h) { in 5..11 -> add(Tag.MORNING); in 17..23 -> add(Tag.EVENING); in 0..4 -> { add(Tag.NIGHT); add(Tag.EVENING) } }
            add(if (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) Tag.WEEKEND else Tag.WEEKDAY)
            add(when (d.monthValue) { 12, 1, 2 -> Tag.WINTER; in 3..5 -> Tag.SPRING; in 6..8 -> Tag.SUMMER; else -> Tag.AUTUMN })
        }
    }

    /** A deed fits the moment if every kind of tag it has (time, week, season) matches. */
    fun fits(b: Badge, ctx: Set<Tag>): Boolean = listOf(TIME, WEEK, SEASON).all { group ->
        val own = b.tags intersect group
        own.isEmpty() || own.any { it in ctx }
    }

    /**
     * Today's agenda: deeds you've done before and may well have done again, deeds that fit the hour
     * and the season, and some you've never been honoured for. The same all day, new tomorrow.
     */
    fun agenda(s: State, catalog: Catalog, now: Long, zone: ZoneId, salt: Long, n: Int = 12): List<Badge> {
        val today = now.day(zone)
        val done = s.on(today).map { it.key }.toSet()
        val ctx = context(now, zone)
        val rnd = Random(salt xor (today * -7046029254386353131L))
        val pool = catalog.items.filter { it.key !in done && fits(it, ctx) }
        val out = LinkedHashSet<Badge>()
        pool.filter { s.count(it.key) > 0 }.sortedByDescending { s.count(it.key) }.take(8).shuffled(rnd).take(4).forEach(out::add)
        pool.filter { it.tags.isNotEmpty() && it !in out }.shuffled(rnd).take(3).forEach(out::add)
        val fresh = pool.filter { it !in out }.shuffled(rnd).sortedBy { if (s.count(it.key) == 0) 0 else 1 }
        for (b in fresh) { if (out.size >= n) break; out.add(b) }
        return out.toList()
    }

    // The felt: a rectangle 300×320 with a point below it, down to 440. Its title takes the top.
    private const val TOP = 96f
    private const val SQUARE = 320f

    private fun halfWidth(y: Float) = if (y <= SQUARE) Rules.PENNANT_W / 2 else (Rules.PENNANT_W / 2) * (Rules.PENNANT_H - y) / (Rules.PENNANT_H - SQUARE)

    /** Whether a badge centred at ([x], [y]) sits wholly on the felt, clear of the title and the fringe. */
    fun onFelt(x: Float, y: Float): Boolean {
        val r = Rules.PIN_R
        if (y - r < TOP || y + r > Rules.PENNANT_H - 40) return false
        return abs(x - Rules.PENNANT_W / 2) + r + 14 <= halfWidth(y + r * .7f)
    }

    /** The nearest spot on the felt to ([x], [y]), for a badge dragged off its edge. */
    fun clamp(x: Float, y: Float): Pair<Float, Float> {
        if (onFelt(x, y)) return x to y
        var best = Rules.PENNANT_W / 2 to 200f
        var bd = Float.MAX_VALUE
        var yy = TOP
        while (yy < Rules.PENNANT_H) {
            var xx = 0f
            while (xx < Rules.PENNANT_W) {
                if (onFelt(xx, yy)) { val d = hypot(xx - x, yy - y); if (d < bd) { bd = d; best = xx to yy } }
                xx += 4f
            }
            yy += 4f
        }
        return best
    }

    /** The roomiest free spot on a pennant: of many tries, the one farthest from every other badge. */
    fun spot(taken: List<Pin>, rnd: Random): Pair<Float, Float> {
        var best = Rules.PENNANT_W / 2 to 200f
        var bd = -1f
        repeat(500) {
            val x = rnd.nextFloat() * Rules.PENNANT_W
            val y = TOP + rnd.nextFloat() * (Rules.PENNANT_H - TOP)
            if (!onFelt(x, y)) return@repeat
            // A slight pull to the middle and upwards, so the first badges don't hide in corners.
            val wall = min(abs(x - Rules.PENNANT_W / 2), 200f) * .12f + (y - TOP) * .05f
            val d = (taken.minOfOrNull { hypot(it.x - x, it.y - y) } ?: 400f) - wall
            if (d > bd) { bd = d; best = x to y }
        }
        return best
    }

    /**
     * The secret badges a batch of awards brings: checked on the state with the batch already counted.
     * [prevAt] is when the award before the batch was made, for a return after a long silence.
     */
    fun specials(s: State, batch: List<String>, now: Long, zone: ZoneId, prevAt: Long?, catalog: Catalog): List<String> {
        val today = now.day(zone)
        val todayKeys = s.on(today).map { it.key }.toSet()
        val d = now.date(zone)
        val h = now.hour(zone)
        val plain = s.awards.filter { !it.key.startsWith("sp:") }
        fun fresh(k: String) = if (k in Specials.once) s.count(k) == 0 else k !in todayKeys
        return buildList {
            if (fresh(Specials.FIRST)) add(Specials.FIRST)
            if (h in 0..4 && fresh(Specials.NIGHT)) add(Specials.NIGHT)
            if (h in 5..6 && fresh(Specials.EARLY)) add(Specials.EARLY)
            if (plain.count { it.day == today } >= 5 && fresh(Specials.SHOCK)) add(Specials.SHOCK)
            if (plain.size >= 100 && fresh(Specials.HUNDRED)) add(Specials.HUNDRED)
            if (prevAt != null && today - prevAt.day(zone) >= Rules.RETURN_DAYS && fresh(Specials.RETURN)) add(Specials.RETURN)
            if (plain.mapNotNull { catalog[it.key]?.dept }.toSet().size == Dept.entries.size && fresh(Specials.ALL)) add(Specials.ALL)
            if (batch.any { it.startsWith("own:") } && fresh(Specials.INVENTOR)) add(Specials.INVENTOR)
            if (batch.any { s.metal(it) == Metal.GOLD } && fresh(Specials.GOLD)) add(Specials.GOLD)
            if (d.monthValue == 1 && d.dayOfMonth == 1 && fresh(Specials.NEW_YEAR)) add(Specials.NEW_YEAR)
            if (d.dayOfMonth == 1 && d.monthValue in setOf(3, 6, 9, 12) && fresh(Specials.SEASON)) add(Specials.SEASON)
        }
    }

    /** A year in numbers, for the commission's annual report. */
    data class Year(val year: Int, val awards: Int, val days: Int, val badges: Int, val survived: Int, val top: List<Pair<String, Int>>, val depts: Map<Dept, Int>)

    fun year(s: State, year: Int, zone: ZoneId, catalog: Catalog): Year {
        val a = s.awards.filter { it.at.date(zone).year == year }
        return Year(
            year, a.size, a.map { it.day }.toSet().size, a.map { it.key }.toSet().size, a.count { it.key == Specials.SURVIVED },
            a.groupingBy { it.key }.eachCount().entries.sortedByDescending { it.value }.take(3).map { it.key to it.value },
            a.mapNotNull { catalog[it.key]?.dept }.groupingBy { it }.eachCount(),
        )
    }
}
