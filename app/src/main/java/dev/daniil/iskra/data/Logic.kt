package dev.daniil.iskra.data

import dev.daniil.iskra.pet.Mood
import java.time.LocalDate
import java.time.LocalDateTime

/** 0 = nothing, 1 = all done, 2 = saved by an ember */
data class DayCell(val date: LocalDate, val state: Int)

data class Summary(
    val streak: Int,
    val best: Int,
    val stage: Int,
    val mood: Mood,
    val done: Int,
    val total: Int,
    val complete: Boolean,
    val week: List<DayCell>,
    val freezes: Int,
    val totalDone: Int,
    val daysDone: Int,
    val palette: Int,
    val petName: String,
)

object Logic {
    val stageNames = listOf("Искорка", "Огонёк", "Пламя", "Феникс")
    val thresholds = listOf(0, 3, 7, 14)

    fun stageFor(streak: Int): Int = thresholds.indexOfLast { streak >= it }

    fun streakEnding(eff: Set<String>, end: LocalDate): Int {
        var d = end
        var n = 0
        while (d.toString() in eff) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    /** Consecutive days ending today (or yesterday, while today is still in progress). */
    fun streak(eff: Set<String>, today: LocalDate): Int =
        if (today.toString() in eff) streakEnding(eff, today) else streakEnding(eff, today.minusDays(1))

    fun summary(s: IskraState, now: LocalDateTime = LocalDateTime.now()): Summary {
        val today = now.toLocalDate()
        val eff = s.effective
        val streak = streak(eff, today)
        val total = s.tasks.size
        val done = s.tasks.count { it.id in s.doneIds }
        val complete = total > 0 && done == total
        val broken = streak == 0 && s.history.isNotEmpty()
        val mood = when {
            complete -> Mood.HAPPY
            broken -> Mood.SMOLDER
            total > 0 && done == 0 && now.hour >= 18 -> Mood.HUNGRY
            else -> Mood.NORMAL
        }
        val week = (6 downTo 0).map { today.minusDays(it.toLong()) }.map {
            val k = it.toString()
            DayCell(it, if (k in s.history) 1 else if (k in s.frozen) 2 else 0)
        }
        return Summary(
            streak = streak,
            best = maxOf(s.best, streak),
            stage = stageFor(streak),
            mood = mood,
            done = done,
            total = total,
            complete = complete,
            week = week,
            freezes = s.freezes,
            totalDone = s.totalDone,
            daysDone = s.history.size,
            palette = s.settings.palette,
            petName = s.settings.petName.ifBlank { "Искра" },
        )
    }

    fun moodText(sm: Summary): String = when {
        sm.total == 0 -> "Добавь дела — ими я питаюсь 🪵"
        sm.mood == Mood.HAPPY -> "Сыта и сияю! День засчитан"
        sm.mood == Mood.SMOLDER -> "Я почти погасла… сделай все дела, чтобы раздуть меня снова"
        sm.mood == Mood.HUNGRY -> "Уже вечер, а я голодная 🥺"
        sm.done == 0 -> "Покорми меня делами"
        else -> "Ещё ${sm.total - sm.done} — и день засчитан"
    }

    fun nextStage(streak: Int): Pair<String, Float> {
        val st = stageFor(streak)
        if (st >= thresholds.lastIndex) return "Высшая форма. Держи серию!" to 1f
        val from = thresholds[st]
        val to = thresholds[st + 1]
        val need = to - streak
        return "До «${stageNames[st + 1]}»: $need ${days(need)}" to (streak - from).toFloat() / (to - from)
    }

    fun days(n: Int): String = plural(n, "день", "дня", "дней")

    fun plural(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m10 == 1 && m100 != 11 -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
    }

    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        in 18..22 -> "Добрый вечер"
        else -> "Доброй ночи"
    }

    private val phrasesHappy = listOf(
        "Я сияю ярче всех! ✨", "Спасибо, я сыта 🔥", "Вот это день!", "Ты лучший кочегар",
        "Тепло-тепло-тепло", "Сегодня можно и отдохнуть", "Я горжусь тобой!",
    )
    private val phrasesNormal = listOf(
        "Ещё чуть-чуть — и день наш", "Тепло… но можно и жарче", "Трр-трр 🔥",
        "Полешко? Полешко!", "Я верю в тебя", "Одно дело — и мне уже лучше",
        "Давай по одному, без спешки",
    )
    private val phrasesHungry = listOf("Я голодная…", "Полешко бы…", "Пш-ш… покорми меня", "Вечер, а я ещё не ела 🥺")
    private val phrasesSmolder = listOf("Кхе-кхе… дым…", "Раздуй меня, пожалуйста", "Я ещё тлею. Не всё потеряно", "Даже феникс горел не сразу")
    private val phrasesMorning = listOf("Доброе утро! Чем покормишь?", "Утро — лучшее время для первого полена", "Проснулась и уже голодная")
    private val phrasesNight = listOf("Тс-с… ночь. Я дремлю", "Сладких снов, кочегар", "Завтра будет новый день")
    private val phrasesTap = listOf("Щекотно!", "Ай, горячо!", "Хи-хи", "Ещё раз!", "Не дуй, погасну!", "Пф-ф 🔥")

    fun phrase(sm: Summary, hour: Int, tap: Boolean): String {
        val list = when {
            tap && Math.random() < 0.4 -> phrasesTap
            sm.mood == Mood.SMOLDER -> phrasesSmolder
            sm.mood == Mood.HAPPY -> phrasesHappy
            sm.mood == Mood.HUNGRY -> phrasesHungry
            hour in 5..10 && sm.done == 0 -> phrasesMorning
            hour >= 23 || hour < 5 -> phrasesNight
            else -> phrasesNormal
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
        "Даже феникс начинал с искорки.",
        "Не сравнивай себя с другими. Сравнивай со вчерашним собой.",
        "Медленно — тоже движение.",
        "Ты здесь — значит, огонь ещё горит.",
        "Сначала дело, потом сомнения.",
        "Привычки — это то, кем ты становишься каждый день.",
    )

    fun quoteOfDay(date: LocalDate): String = quotes[(date.dayOfYear + date.year) % quotes.size]

    val suggestions = listOf(
        "💧" to "Выпить 2 литра воды",
        "🏃" to "Зарядка 10 минут",
        "📖" to "Прочитать 10 страниц",
        "🧘" to "Медитация 5 минут",
        "🇬🇧" to "Английский 15 минут",
        "🚶" to "Прогулка",
        "🛏️" to "Лечь до полуночи",
        "📵" to "Час без телефона",
        "🥗" to "Нормально поесть",
        "💻" to "Час над своим проектом",
    )

    val emojis = listOf(
        "✨", "💧", "🏃", "📖", "🧘", "🇬🇧", "🚶", "🛏️", "📵", "🥗", "💻", "🎯",
        "💪", "🧹", "🎸", "✍️", "💊", "🦷", "🌱", "🐶", "📞", "💰", "🎨", "🧠",
    )
}
