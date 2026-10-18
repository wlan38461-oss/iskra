package dev.daniil.iskra.data

import dev.daniil.iskra.pet.Mood
import java.time.LocalDate
import kotlin.random.Random

object Texts {

    fun plural(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m10 == 1 && m100 != 11 -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
    }

    fun days(n: Int): String = plural(n, "день", "дня", "дней")
    fun tasks(n: Int): String = plural(n, "дело", "дела", "дел")

    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        in 18..22 -> "Добрый вечер"
        else -> "Доброй ночи"
    }

    fun time(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

    // ------------------------------------------------------------- the pet

    fun formName(sm: Summary): String = Growth.names[sm.form]

    fun moodText(sm: Summary): String {
        if (sm.form == 0) {
            val left = Growth.HATCH - sm.lit
            return when {
                sm.total == 0 -> "Добавь дела — их тепло согреет яйцо 🥚"
                sm.complete -> "Яйцо тёплое! Ещё $left ${days(left)} — и я вылуплюсь"
                sm.mood == Mood.SMOLDER -> "Яйцо остывает… закрой сегодня все дела"
                left == 1 -> "Ещё ${sm.left} ${tasks(sm.left)} — и я вылуплюсь! 🐣"
                else -> "Ещё ${sm.left} ${tasks(sm.left)} — и день засчитан"
            }
        }
        return when {
            sm.total == 0 -> "Добавь дела — ими я питаюсь 🔥"
            sm.mood == Mood.HAPPY -> "Сыта и сияю! День засчитан"
            sm.mood == Mood.SMOLDER -> "Я почти погасла… сделай все дела, чтобы раздуть меня снова"
            sm.mood == Mood.HUNGRY -> "Уже вечер, а я голодная 🥺"
            sm.done == 0 -> "Покорми меня делами"
            else -> "Ещё ${sm.left} ${tasks(sm.left)} — и день засчитан"
        }
    }

    fun growthText(sm: Summary): String {
        val next = Growth.next(sm.lit)
        return when {
            sm.form == 0 -> {
                val left = Growth.HATCH - sm.lit
                "До вылупления: $left ${days(left)}"
            }
            next == null -> "Высшая форма · ${sm.lit} ${days(sm.lit)} горения"
            else -> "До формы «${next.first}»: ${next.second} ${days(next.second)}"
        }
    }

    private val happy = listOf(
        "Я сияю ярче всех! ✨", "Спасибо, я сыта 🔥", "Вот это день!", "Ты лучший кочегар",
        "Тепло-тепло-тепло", "Сегодня можно и отдохнуть", "Я горжусь тобой!", "Чувствую, что расту!",
    )
    private val normal = listOf(
        "Ещё чуть-чуть — и день наш", "Тепло… но можно и жарче", "Трр-трр 🔥", "Полешко? Полешко!",
        "Я верю в тебя", "Одно дело — и мне уже лучше", "Давай по одному, без спешки", "Как дела? Покормишь?",
    )
    private val hungry = listOf("Я голодная…", "Полешко бы…", "Пш-ш… покорми меня", "Вечер, а я ещё не ела 🥺")
    private val smolder = listOf("Кхе-кхе… дым…", "Раздуй меня, пожалуйста", "Я ещё тлею. Не всё потеряно", "Даже феникс горел не сразу")
    private val morning = listOf("Доброе утро! Чем покормишь?", "Утро — лучшее время для первого полена", "Проснулась и уже голодная")
    private val night = listOf("Тс-с… ночь. Я дремлю", "Сладких снов, кочегар", "Завтра будет новый день")
    private val tap = listOf("Щекотно!", "Ай, горячо!", "Хи-хи", "Ещё раз!", "Не дуй, погасну!", "Пф-ф 🔥", "Ты чего? 😳")
    private val egg = listOf(
        "Тук-тук… 🥚", "Мне тепло, когда ты делаешь дела", "Кажется, я шевелюсь…",
        "Я слышу, как ты стараешься", "Скоро увидимся!", "Внутри так уютно",
    )
    private val eggTap = listOf("*шевелится*", "Тук!", "Тепло-о…", "Хрусь?", "Ой!")
    private val eggCold = listOf("Брр… холодно…", "Согрей меня делами", "Я остываю…")

    fun phrase(sm: Summary, hour: Int, tapped: Boolean): String {
        val list = when {
            sm.form == 0 && tapped && Random.nextFloat() < 0.5f -> eggTap
            sm.form == 0 && sm.mood == Mood.SMOLDER -> eggCold
            sm.form == 0 -> egg
            tapped && Random.nextFloat() < 0.4f -> tap
            sm.mood == Mood.SMOLDER -> smolder
            sm.mood == Mood.HAPPY -> happy
            sm.mood == Mood.HUNGRY -> hungry
            hour in 5..10 && sm.done == 0 -> morning
            hour >= 23 || hour < 5 -> night
            else -> normal
        }
        return list.random()
    }

    private val quotes = listOf(
        "Маленькие дела каждый день сильнее больших раз в месяц.",
        "Не надо быть идеальным. Надо просто не гаснуть.",
        "Сегодняшнее полено — завтрашнее тепло.",
        "Огонь не ждёт настроения. Он просто горит.",
        "Один шаг сегодня лучше десяти «потом».",
        "Серия строится по одному дню.",
        "Начни с самого маленького дела.",
        "Устал? Сделай хотя бы одно. Это уже победа.",
        "Сложно только первые пять минут.",
        "Прогресс тихий. Но он есть.",
        "Каждое «сделано» — кирпичик в твоё завтра.",
        "Мотивация приходит во время дела, а не до него.",
        "Огонь любит регулярность.",
        "Хороший день складывается из маленьких побед.",
        "Сделай сегодня то, за что завтра скажешь себе спасибо.",
        "Пропустить день можно. Главное — не два подряд.",
        "Даже феникс начинал с яйца.",
        "Не сравнивай себя с другими. Сравнивай со вчерашним собой.",
        "Медленно — тоже движение.",
        "Ты здесь — значит, огонь ещё горит.",
        "Сначала дело, потом сомнения.",
        "Привычки — это то, кем ты становишься каждый день.",
    )

    fun quoteOfDay(date: LocalDate): String = quotes[(date.dayOfYear + date.year) % quotes.size]

    // ------------------------------------------------------------- tasks

    val suggestions = listOf(
        "💧" to "Выпить 2 литра воды",
        "🏃" to "Зарядка 10 минут",
        "📖" to "Прочитать 10 страниц",
        "🧘" to "Медитация 5 минут",
        "🇬🇧" to "Английский 15 минут",
        "🚶" to "Прогулка 30 минут",
        "🛏️" to "Лечь до полуночи",
        "📵" to "Час без телефона",
        "🥗" to "Нормально поесть",
        "💻" to "Час над своим проектом",
        "🦷" to "Почистить зубы",
        "💊" to "Витамины",
        "🧹" to "Убраться 10 минут",
        "✍️" to "Записать 3 мысли",
        "☎️" to "Позвонить близким",
        "🍎" to "Съесть фрукт",
    )

    class EmojiCat(val name: String, val icon: String, val items: List<String>)

    val emojiCats = listOf(
        EmojiCat(
            "Здоровье", "💧",
            listOf(
                "💧", "🥗", "🍎", "🥦", "🥕", "🍌", "🍳", "🥛", "🍵", "☕", "🥤", "🚭",
                "💊", "🦷", "🛏️", "😴", "🚿", "🛁", "🧼", "🧴", "🌡️", "❤️", "🌞", "🧘",
            ),
        ),
        EmojiCat(
            "Спорт", "🏃",
            listOf(
                "🏃", "🚶", "🏋️", "💪", "🚴", "🏊", "🧗", "🤸", "⚽", "🏀", "🏐", "🎾",
                "🏓", "🏸", "🥊", "🥋", "⛷️", "🏂", "🛹", "⛹️", "🤾", "🏆", "🥇", "👟",
            ),
        ),
        EmojiCat(
            "Учёба", "📚",
            listOf(
                "📖", "📚", "✏️", "📝", "✍️", "🧠", "🎓", "🇬🇧", "🌍", "🔤", "🧮", "🔬",
                "🧪", "🔭", "💡", "📐", "📓", "🗒️", "🎧", "📰", "🗣️", "💬", "🤔", "🖊️",
            ),
        ),
        EmojiCat(
            "Работа", "💻",
            listOf(
                "💻", "🖥️", "⌨️", "📱", "📈", "📊", "💼", "📧", "📞", "📅", "⏰", "🗂️",
                "📌", "✅", "🔍", "🧾", "💰", "💳", "🏦", "📦", "🛠️", "🔧", "🚀", "🎯",
            ),
        ),
        EmojiCat(
            "Дом", "🏠",
            listOf(
                "🧹", "🧺", "🧽", "🍽️", "🛒", "🍲", "🥘", "🧊", "🗑️", "👕", "🧦", "🛋️",
                "🔑", "🏠", "🚗", "🔌", "🌱", "🌿", "💐", "🐶", "🐱", "🐟", "🐾", "🪑",
            ),
        ),
        EmojiCat(
            "Хобби", "🎨",
            listOf(
                "🎸", "🎹", "🎻", "🥁", "🎷", "🎺", "🎤", "🎵", "🎨", "🖌️", "📷", "📸",
                "🎬", "🎮", "🎲", "♟️", "🧩", "🧶", "🧵", "✂️", "🎭", "🎣", "🏕️", "🌄",
            ),
        ),
        EmojiCat(
            "Отдых", "🌙",
            listOf(
                "🌙", "☀️", "🌈", "🌊", "🏖️", "⛰️", "🌳", "🌸", "🍃", "🔥", "✨", "⭐",
                "🎉", "🎁", "🕯️", "📵", "😊", "🙏", "💤", "🍿", "🎈", "🧸", "🛀", "☁️",
            ),
        ),
        EmojiCat(
            "Близкие", "💕",
            listOf(
                "👪", "💕", "🤗", "😘", "👵", "👴", "👶", "🧒", "🎂", "💌", "🤝", "👋",
                "🥰", "😍", "💝", "💞", "🙌", "👏", "🎊", "🍰", "☎️", "✉️", "🏡", "🌹",
            ),
        ),
    )

    // ------------------------------------------------------------- notifications

    fun morningTitle(): String = listOf("☀️ Доброе утро!", "🌅 Новый день — новые поленья", "☕ Утро! План на сегодня").random()

    fun morningText(sm: Summary, emojis: String): String =
        "Сегодня ${sm.total} ${tasks(sm.total)}: $emojis " +
            if (sm.form == 0) "Яйцо ждёт тепла 🥚" else "${sm.petName} ждёт поленьев"

    fun eveningTitle(sm: Summary): String =
        if (sm.form == 0) "🥚 Яйцо остывает" else "🔥 ${sm.petName} ждёт поленьев"

    fun eveningText(sm: Summary): String = buildString {
        append("Осталось ${sm.left} ${tasks(sm.left)} — и день засчитан.")
        if (sm.streak > 0) append(" Серия: ${sm.streak} ${days(sm.streak)}")
    }

    fun lastTitle(sm: Summary): String =
        if (sm.streak > 0) "⏳ Серия в ${sm.streak} ${days(sm.streak)} сгорит в полночь" else "⏳ День почти закончился"

    fun lastText(sm: Summary): String = buildString {
        append("Осталось ${sm.left} ${tasks(sm.left)}. Успеешь? 🔥")
        if (sm.freezes > 0 && sm.streak > 0) append(" Уголёк спасёт серию, но лучше его поберечь")
    }

    fun taskText(petName: String): String = listOf(
        "$petName ждёт это полено 🔥",
        "Пора! Одно дело — и огонь ярче",
        "Сделаешь — нажми «Готово» прямо тут",
        "Маленький шаг — большое тепло",
    ).random()
}
