package app.honours.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The ministries a deed belongs to; each has its own enamel. */
enum class Dept(val enamel: Int) { HOME(0), BODY(1), PEOPLE(2), CITY(3), BRAVE(4), REST(5), MIND(6) }

enum class Shape { STAR, ROUND, SHIELD, FLAG, RHOMB, GEAR, MEDAL, WREATH, ORDER }

/** When a deed tends to happen; the commission offers it more often then. */
enum class Tag { MORNING, EVENING, NIGHT, WEEKEND, WEEKDAY, WINTER, SPRING, SUMMER, AUTUMN }

enum class Gender { HE, SHE }

/** The rim and relief of a badge, by class: III bronze, II silver, I gold. */
enum class Metal(val roman: String) { BRONZE("III"), SILVER("II"), GOLD("I") }

enum class Source { CATALOG, OWN, SPECIAL }

/** Text in both languages; own badges carry the same words in both. */
data class Tx(val ru: String, val en: String) {
    companion object { fun same(s: String) = Tx(s, s) }
}

/**
 * How a badge looks: its outline, two enamels from [Look.ENAMELS], and either a stamped pictogram from
 * the icon set or a 9×9 pixel drawing (digits 0–7, 0 empty) made in the workshop.
 */
data class Look(val shape: Shape, val enamel: Int, val band: Int, val icon: String?, val pixels: String? = null) {
    companion object {
        const val ENAMELS = 8
        const val GRID = 9
    }
}

/** One badge, whatever made it: the factory catalogue, the workshop, or the commission's own secret list. */
data class Badge(
    val key: String,
    val source: Source,
    val dept: Dept?,
    val look: Look,
    /** The plain deed as ticked in the commission. */
    val deed: Tx,
    /** The grand title. */
    val name: Tx,
    /** «За …» — what the order says. */
    val decree: Tx,
    val tags: Set<Tag> = emptySet(),
)

/** Every award ever made, one row per deed per day. */
@Entity(tableName = "award")
data class Award(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val at: Long,
    val day: Long,
)

/** A badge pinned to a pennant at a spot in pennant units (see [Rules.PENNANT_W]). */
@Entity(tableName = "pin", primaryKeys = ["pennant", "key"])
data class Pin(val pennant: Int, val key: String, val x: Float, val y: Float, val at: Long)

/** A badge made in the workshop; awarded like any other under the key "own:<id>". */
@Entity(tableName = "own")
data class Own(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val deed: String,
    val decree: String,
    val shape: Shape,
    val enamel: Int,
    val band: Int,
    val icon: String?,
    val pixels: String?,
    val createdAt: Long,
    val archived: Boolean = false,
) {
    val key get() = "own:$id"
}

/** A badge sent to someone else on an award sheet. */
@Entity(tableName = "given")
data class Given(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val to: String,
    val at: Long,
)

object Rules {
    /** Pennant drawing units: the felt is 300 wide and 440 tall, point down. */
    const val PENNANT_W = 300f
    const val PENNANT_H = 440f
    /** A pinned badge's radius on the felt. */
    const val PIN_R = 25f
    /** Badges on one pennant before a new one is hung. */
    const val PER_PENNANT = 16
    /** Awards of the same badge to reach class II and class I. */
    const val SILVER = 5
    const val GOLD = 15
    /** Awards that make the holder «Заслуженный». */
    const val HONOURED = 30
    /** Days of silence that make the next award a return. */
    const val RETURN_DAYS = 30

    fun metal(count: Int) = when {
        count >= GOLD -> Metal.GOLD
        count >= SILVER -> Metal.SILVER
        else -> Metal.BRONZE
    }
}

class State(val awards: List<Award>, val pins: List<Pin>, val own: List<Own>, val given: List<Given>) {
    val counts: Map<String, Int> = awards.groupingBy { it.key }.eachCount()
    fun count(key: String) = counts[key] ?: 0
    fun metal(key: String) = Rules.metal(count(key))

    /** The pennant now on the wall; earlier ones hang beside it. */
    val pennant: Int = pins.maxOfOrNull { it.pennant } ?: 0
    fun pinsOn(p: Int) = pins.filter { it.pennant == p }

    fun on(day: Long) = awards.filter { it.day == day }
    fun first(key: String) = awards.firstOrNull { it.key == key }
    fun last(key: String) = awards.lastOrNull { it.key == key }
    fun ownBadge(id: Long) = own.firstOrNull { it.id == id }

    companion object { val EMPTY = State(emptyList(), emptyList(), emptyList(), emptyList()) }
}

fun Long.day(zone: ZoneId): Long = Instant.ofEpochMilli(this).atZone(zone).toLocalDate().toEpochDay()
fun Long.date(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
fun Long.hour(zone: ZoneId): Int = Instant.ofEpochMilli(this).atZone(zone).hour
