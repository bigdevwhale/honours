package app.honours.data

import app.honours.domain.Award
import app.honours.domain.Catalog
import app.honours.domain.Engine
import app.honours.domain.Given
import app.honours.domain.Own
import app.honours.domain.Pin
import app.honours.domain.Rules
import app.honours.domain.Specials
import app.honours.domain.State
import app.honours.domain.day
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import kotlin.random.Random

/** One badge just awarded, with what it changed: its class, and whether it went onto a pennant. */
data class Awarded(val key: String, val id: Long, val count: Int, val up: Boolean, val pin: Pin?, val newPennant: Boolean)

/**
 * The commission's records: awards, pins, workshop badges and award sheets in Room. Every change reads the
 * state, applies a rule from [Engine] and publishes the new state to [state].
 */
class Repo(private val db: HonoursDb, val catalog: Catalog) {
    private val dao = db.dao()
    private val mutex = Mutex()
    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state

    /** The clock and dice; the demo seed winds them to replay months. */
    var clock: () -> Long = System::currentTimeMillis
    var rnd: Random = Random.Default
    var zone: ZoneId = ZoneId.systemDefault()

    suspend fun refresh(): State = State(dao.awards(), dao.pins(), dao.own(), dao.given()).also { _state.value = it }

    private suspend fun <T> op(block: suspend (State, Long) -> T): T = mutex.withLock {
        val s = refresh()
        block(s, clock()).also { refresh() }
    }

    /**
     * Awards the deeds ticked today, then whatever secret badges they bring. A deed already awarded today is
     * skipped. A badge earned for the first time goes onto the current pennant, or a fresh one when it's full.
     */
    suspend fun award(keys: List<String>): List<Awarded> = op { s0, now ->
        val today = now.day(zone)
        val done = s0.on(today).map { it.key }.toSet()
        val batch = keys.distinct().filter { it !in done }
        if (batch.isEmpty()) return@op emptyList()
        val prevAt = s0.awards.lastOrNull()?.at
        val out = mutableListOf<Awarded>()
        for (k in batch) out += give(k, now)
        val s1 = refresh()
        for (k in Engine.specials(s1, batch, now, zone, prevAt, catalog)) out += give(k, now)
        val s2 = refresh()
        if (s2.pennant >= 1 && s2.count(Specials.BANNER) == 0) out += give(Specials.BANNER, now)
        out
    }

    /** The order for a day that went badly or held nothing: once a day, and only on its own. */
    suspend fun survived(): Awarded? = op { s, now ->
        if (s.on(now.day(zone)).any { it.key == Specials.SURVIVED }) null else give(Specials.SURVIVED, now)
    }

    private suspend fun give(key: String, now: Long): Awarded {
        val s = refresh()
        val before = s.count(key)
        val id = dao.insert(Award(key = key, at = now, day = now.day(zone)))
        var pin: Pin? = null
        var newPennant = false
        // A badge is pinned once, where it was first earned; later awards only raise its class.
        if (s.pins.none { it.key == key }) {
            val full = s.pinsOn(s.pennant).size >= Rules.PER_PENNANT
            val p = if (full) s.pennant + 1 else s.pennant
            newPennant = full
            val (x, y) = Engine.spot(s.pinsOn(p), rnd)
            pin = Pin(p, key, x, y, now).also { dao.pin(it) }
        }
        return Awarded(key, id, before + 1, before > 0 && Rules.metal(before) != Rules.metal(before + 1), pin, newPennant)
    }

    /** Moves a badge on its pennant, kept on the felt. */
    suspend fun move(pennant: Int, key: String, x: Float, y: Float) = op { s, _ ->
        val p = s.pins.firstOrNull { it.pennant == pennant && it.key == key } ?: return@op
        val (cx, cy) = Engine.clamp(x, y)
        dao.pin(p.copy(x = cx, y = cy))
    }

    /** Takes back an award made by mistake; a badge left with no awards comes off the pennant. */
    suspend fun revoke(id: Long) = op { s, _ ->
        val a = s.awards.firstOrNull { it.id == id } ?: return@op
        dao.deleteAward(id)
        if (s.count(a.key) <= 1) s.pins.filter { it.key == a.key }.forEach { dao.unpin(it.pennant, it.key) }
    }

    suspend fun saveOwn(o: Own): Long = op { _, now -> if (o.id == 0L) dao.insert(o.copy(createdAt = now)) else { dao.update(o); o.id } }

    /** Retires a workshop badge from the agenda; what was awarded stays awarded. */
    suspend fun archive(id: Long) = op { s, _ -> s.ownBadge(id)?.let { dao.update(it.copy(archived = true)) } }

    /** An award sheet sent to someone else; the first one brings a badge of its own. */
    suspend fun given(key: String, to: String): Awarded? = op { s, now ->
        dao.insert(Given(key = key, to = to.trim(), at = now))
        if (s.count(Specials.GIVER) == 0) give(Specials.GIVER, now) else null
    }

    /** Raw writes for the demo seed. */
    suspend fun put(a: Award) = mutex.withLock { dao.insert(a) }
}
