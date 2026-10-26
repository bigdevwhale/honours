package app.honours.ui

import app.honours.domain.Badge
import app.honours.domain.Dept
import app.honours.domain.Gender
import app.honours.domain.Metal
import app.honours.domain.Tx
import java.time.LocalDate
import kotlin.math.abs

/** The commission's words that come from data rather than resources, in both languages. */
class Words(val ru: Boolean, val gender: Gender?) {
    fun t(r: String, e: String) = if (ru) g(r) else e
    fun t(x: Tx) = t(x.ru, x.en)

    /** Picks the form in Russian {he|she} marks; without a known gender both show, as «помыл(а)». */
    fun g(s: String): String = MARK.replace(s) { m ->
        val (he, she) = m.destructured
        when (gender) {
            Gender.HE -> he
            Gender.SHE -> she
            null -> if (she.startsWith(he)) "$he(${she.removePrefix(he)})" else "$he/$she"
        }
    }

    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = abs(n) % 100; val b = a % 10
        return when { a in 11..19 -> many; b == 1 -> one; b in 2..4 -> few; else -> many }
    }

    fun n(n: Int, one: String, few: String, many: String, en1: String, enN: String) = if (ru) "$n ${plural(n, one, few, many)}" else "$n ${if (n == 1) en1 else enN}"

    fun name(b: Badge) = t(b.name)

    /** The title in quotes, unless it already carries its own: «Укротитель сковороды», but Медаль «Шаг вперёд». */
    fun quoted(b: Badge): String {
        val n = name(b)
        return if ('«' in n || '“' in n || '"' in n) n else if (ru) "«$n»" else "“$n”"
    }
    fun deed(b: Badge) = t(b.deed)
    fun decree(b: Badge) = t(b.decree)

    fun metal(m: Metal) = if (ru) "${m.roman} степени" else "Class ${m.roman}"
    fun metalName(m: Metal) = when (m) { Metal.BRONZE -> t("бронза", "bronze"); Metal.SILVER -> t("серебро", "silver"); Metal.GOLD -> t("золото", "gold") }

    /** The award in the case an order needs: «медалью „…“», «орденом „…“», or «знаком „…“» for a plain title. */
    fun with(b: Badge): String {
        val n = name(b)
        if (!ru) return when {
            n.startsWith("The ") -> "the ${n.removePrefix("The ")}"
            Regex("\\b(Order|Medal|Badge|Cross|Star|Ribbon)\\b").containsMatchIn(n) -> "the $n"
            else -> "the badge “$n”"
        }
        for ((nom, ins) in listOf("Медаль " to "медалью ", "Орден " to "орденом ", "Знак " to "знаком ")) if (n.startsWith(nom)) return ins + n.removePrefix(nom)
        return "знаком «$n»"
    }

    /** The closing words of an order: «— наградить медалью „…“ II степени». */
    fun resolution(b: Badge, m: Metal) = if (ru) "наградить ${with(b)} ${metal(m)}" else "to be awarded ${with(b)}, ${metal(m)}"

    fun dept(d: Dept) = when (d) {
        Dept.HOME -> t("Быт", "Household"); Dept.BODY -> t("Тело", "Body"); Dept.PEOPLE -> t("Люди", "People")
        Dept.CITY -> t("Вылазки", "Out & about"); Dept.BRAVE -> t("Смелость", "Courage"); Dept.REST -> t("Заслуженный отдых", "Earned rest")
        Dept.MIND -> t("Ум и руки", "Mind & hands")
    }

    /** The ministry's full title, for headers. */
    fun ministry(d: Dept) = when (d) {
        Dept.HOME -> t("Ведомство домашнего хозяйства", "Ministry of the Household")
        Dept.BODY -> t("Ведомство телесного благополучия", "Ministry of Bodily Welfare")
        Dept.PEOPLE -> t("Ведомство людей и связей", "Ministry of People and Ties")
        Dept.CITY -> t("Ведомство малых вылазок", "Ministry of Small Expeditions")
        Dept.BRAVE -> t("Ведомство мелкой отваги", "Ministry of Minor Valour")
        Dept.REST -> t("Ведомство заслуженного безделья", "Ministry of Deserved Idleness")
        Dept.MIND -> t("Ведомство ума и умелых рук", "Ministry of Mind and Handiwork")
    }

    fun date(d: LocalDate) = if (ru) "${d.dayOfMonth} ${MONTHS_GEN[d.monthValue - 1]}" else "${MONTHS_EN[d.monthValue - 1]} ${d.dayOfMonth}"
    fun dateY(d: LocalDate) = if (ru) "${d.dayOfMonth} ${MONTHS_GEN[d.monthValue - 1]} ${d.year} г." else "${MONTHS_EN[d.monthValue - 1]} ${d.dayOfMonth}, ${d.year}"
    fun month(d: LocalDate) = if (ru) MONTHS[d.monthValue - 1] else MONTHS_EN[d.monthValue - 1]
    fun weekday(d: LocalDate) = if (ru) WEEKDAYS[d.dayOfWeek.value - 1] else WEEKDAYS_EN[d.dayOfWeek.value - 1]

    fun times(n: Int) = if (ru) n(n, "раз", "раза", "раз", "", "") else if (n == 1) "once" else "$n times"

    fun pennant(n: Int, year: Int) = if (ru) "ВЫМПЕЛ № ${n + 1} · $year" else "PENNANT No. ${n + 1} · $year"
    val motto get() = t("ЗА МАЛЫЕ ПОДВИГИ", "FOR SMALL DEEDS")

    private companion object {
        val MARK = Regex("\\{([^|{}]*)\\|([^|{}]*)\\}")
        val MONTHS = listOf("январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь")
        val MONTHS_GEN = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
        val MONTHS_EN = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val WEEKDAYS = listOf("понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье")
        val WEEKDAYS_EN = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    }
}
