package app.honours.domain

import java.io.InputStream
import kotlin.math.abs

/** The factory list of small deeds, read from assets/feats.tsv. */
class Catalog(val items: List<Badge>) {
    private val byKey = items.associateBy { it.key }
    operator fun get(key: String) = byKey[key]

    companion object {
        /** Which second enamel goes well with each first one, by index of [Look.ENAMELS]. */
        private val BANDS = listOf(
            listOf(5, 6, 3), listOf(5, 6, 0), listOf(5, 3, 6), listOf(0, 5, 6),
            listOf(0, 6, 5), listOf(0, 2, 3), listOf(0, 3, 4), listOf(6, 5, 4),
        )

        fun band(enamel: Int, key: String) = BANDS[enamel].let { it[abs(key.hashCode()) % it.size] }

        fun read(input: InputStream): Catalog = Catalog(
            input.bufferedReader(Charsets.UTF_8).readLines()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { line ->
                    val c = line.split('\t')
                    val dept = Dept.valueOf(c[1].trim())
                    val tags = if (c[4].trim() == "-") emptySet() else c[4].split(',').map { Tag.valueOf(it.trim().uppercase()) }.toSet()
                    Badge(
                        key = c[0].trim(), source = Source.CATALOG, dept = dept,
                        look = Look(Shape.valueOf(c[2].trim()), dept.enamel, band(dept.enamel, c[0]), c[3].trim()),
                        deed = Tx(c[5], c[6]), name = Tx(c[7], c[8]), decree = Tx(c[9], c[10]), tags = tags,
                    )
                }
        )
    }
}

/**
 * The commission's own badges, never in the list to tick: they find you. Some come once in a life
 * ([once]), the rest may come again and grow in class like any other.
 */
object Specials {
    const val SURVIVED = "sp:survived"
    const val FIRST = "sp:first"
    const val NIGHT = "sp:night"
    const val EARLY = "sp:early"
    const val SHOCK = "sp:shock"
    const val HUNDRED = "sp:hundred"
    const val RETURN = "sp:return"
    const val ALL = "sp:all"
    const val BANNER = "sp:banner"
    const val INVENTOR = "sp:inventor"
    const val GIVER = "sp:giver"
    const val GOLD = "sp:gold"
    const val NEW_YEAR = "sp:newyear"
    const val SEASON = "sp:season"

    val once = setOf(FIRST, HUNDRED, ALL, BANNER, INVENTOR, GIVER, GOLD)

    private fun b(key: String, shape: Shape, enamel: Int, band: Int, icon: String, deed: Tx, name: Tx, decree: Tx) =
        Badge(key, Source.SPECIAL, null, Look(shape, enamel, band, icon), deed, name, decree)

    val all: List<Badge> = listOf(
        b(SURVIVED, Shape.ORDER, 7, 6, "candle",
            Tx("Прожил{|а} этот день", "Got through this day"),
            Tx("Орден Пережитого Дня", "Order of the Day Survived"),
            Tx("За то, что этот день прожит до конца, как бы он ни сложился", "For seeing this day through to its end, however it went")),
        b(FIRST, Shape.STAR, 0, 6, "star",
            Tx("Первый значок", "The first badge"),
            Tx("Знак «Почин»", "The Beginning"),
            Tx("За почин, положенный в деле признания малых подвигов", "For making a start in the noble cause of honouring small deeds")),
        b(NIGHT, Shape.ROUND, 3, 6, "moon",
            Tx("Награда глубокой ночью", "An award in the dead of night"),
            Tx("Ночной дозор", "Night Watch"),
            Tx("За несение службы в часы, когда весь город спит", "For keeping watch in the hours when the whole city sleeps")),
        b(EARLY, Shape.ROUND, 6, 0, "sun",
            Tx("Награда до семи утра", "An award before seven in the morning"),
            Tx("Ранняя пташка", "Early Bird"),
            Tx("За подвиги, совершённые раньше, чем проснулось солнце", "For deeds done before the sun was properly up")),
        b(SHOCK, Shape.GEAR, 0, 6, "star",
            Tx("Пять наград за один день", "Five awards in one day"),
            Tx("{Ударник|Ударница} малого труда", "Shock Worker of Small Labour"),
            Tx("За перевыполнение плана по малым подвигам в течение одного дня", "For overfulfilling the plan of small deeds within a single day")),
        b(HUNDRED, Shape.WREATH, 7, 6, "star",
            Tx("Сотая награда", "The hundredth award"),
            Tx("Знак «Сто подвигов»", "A Hundred Deeds"),
            Tx("За сто подвигов, каждый из которых кто-то другой счёл бы мелочью", "For a hundred deeds, each of which someone else would call a trifle")),
        b(RETURN, Shape.FLAG, 2, 5, "door",
            Tx("Вернул{ся|ась} после долгой тишины", "Came back after a long silence"),
            Tx("Знак «Возвращение»", "The Return"),
            Tx("За возвращение — без объяснений, без упрёков, вовремя", "For coming back: no explanations, no reproaches, right on time")),
        b(ALL, Shape.STAR, 2, 0, "house",
            Tx("Значки всех семи ведомств", "Badges from all seven ministries"),
            Tx("{Многостаночник|Многостаночница}", "Jack of All Ministries"),
            Tx("За разносторонность, проявленную во всех семи ведомствах малых дел", "For versatility shown across all seven ministries of small deeds")),
        b(BANNER, Shape.FLAG, 0, 6, "star",
            Tx("Заполнен первый вымпел", "The first pennant filled"),
            Tx("{Знаменосец|Знаменосица}", "Standard-Bearer"),
            Tx("За первый вымпел, сплошь покрытый знаками отличия", "For the first pennant covered from edge to edge in honours")),
        b(INVENTOR, Shape.GEAR, 6, 3, "pencil",
            Tx("Вручён первый собственный знак", "The first badge of your own awarded"),
            Tx("{Изобретатель|Изобретательница}", "Inventor"),
            Tx("За рационализаторское предложение в области наградного дела", "For an innovative proposal in the field of award-making")),
        b(GIVER, Shape.MEDAL, 2, 5, "gift",
            Tx("Награжден{|а} кто-то другой", "Someone else was honoured"),
            Tx("Наградной отдел", "Awards Department"),
            Tx("За то, что заметил{|а} чужой малый подвиг и не промолчал{|а}", "For noticing someone else's small deed and not keeping quiet")),
        b(GOLD, Shape.WREATH, 6, 0, "star",
            Tx("Первый знак I степени", "The first badge of Class I"),
            Tx("Золотой фонд", "The Gold Reserve"),
            Tx("За упорство, доведённое до первой степени", "For persistence carried all the way to Class I")),
        b(NEW_YEAR, Shape.STAR, 1, 5, "snow",
            Tx("Награда первого января", "An award on the first of January"),
            Tx("Знак «Первый день года»", "First Day of the Year"),
            Tx("За подвиг, открывший собой целый год", "For a deed that opened an entire year")),
        b(SEASON, Shape.RHOMB, 1, 6, "leaf",
            Tx("Награда в первый день сезона", "An award on the first day of a season"),
            Tx("Знак «Сезон открыт»", "Season Opened"),
            Tx("За торжественное открытие нового времени года", "For the ceremonial opening of a new season")),
    )

    private val byKey = all.associateBy { it.key }
    operator fun get(key: String) = byKey[key]
}

/** Finds any badge by key: catalogue, workshop or special. */
class Registry(private val catalog: Catalog, own: List<Own>) {
    private val owned = own.associateBy { it.key }

    operator fun get(key: String): Badge? = catalog[key] ?: Specials[key] ?: owned[key]?.let(::badge)

    companion object {
        fun badge(o: Own) = Badge(
            o.key, Source.OWN, null, Look(o.shape, o.enamel, o.band, o.icon, o.pixels),
            Tx.same(o.deed.ifBlank { o.name }), Tx.same(o.name), Tx.same(o.decree),
        )
    }
}
