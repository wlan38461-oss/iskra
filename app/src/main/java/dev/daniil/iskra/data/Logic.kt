package dev.daniil.iskra.data

import dev.daniil.iskra.pet.Mood
import java.time.LocalDate
import java.time.LocalDateTime

data class Summary(
    val streak: Int,
    val best: Int,
    val stage: Int,
    val mood: Mood,
    val done: Int,
    val total: Int,
    val complete: Boolean,
    val week: List<Pair<LocalDate, Boolean>>,
)

object Logic {
    val stageNames = listOf("Искорка", "Огонёк", "Пламя", "Феникс")
    private val thresholds = listOf(0, 3, 7, 14)

    fun stageFor(streak: Int): Int = thresholds.indexOfLast { streak >= it }

    /** Consecutive complete days ending today (or yesterday, if today is still in progress). */
    fun streak(history: Set<String>, today: LocalDate): Int {
        var d = if (today.toString() in history) today else today.minusDays(1)
        var n = 0
        while (d.toString() in history) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    fun summary(s: IskraState, now: LocalDateTime = LocalDateTime.now()): Summary {
        val today = now.toLocalDate()
        val streak = streak(s.history, today)
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
        val week = (6 downTo 0).map { today.minusDays(it.toLong()) }
            .map { it to (it.toString() in s.history) }
        return Summary(streak, maxOf(s.best, streak), stageFor(streak), mood, done, total, complete, week)
    }

    fun moodText(sm: Summary): String = when {
        sm.total == 0 -> "Добавь дела — ими я питаюсь 🪵"
        sm.mood == Mood.HAPPY -> "Сыта и сияю! День засчитан"
        sm.mood == Mood.SMOLDER -> "Я почти погасла… сделай все дела, чтобы раздуть меня снова"
        sm.mood == Mood.HUNGRY -> "Уже вечер, а я голодная 🥺"
        sm.done == 0 -> "Покорми меня делами"
        else -> "Ещё ${sm.total - sm.done} — и день засчитан"
    }

    fun nextStageText(streak: Int): String {
        val st = stageFor(streak)
        if (st >= thresholds.lastIndex) return "Высшая форма. Держи серию!"
        val need = thresholds[st + 1] - streak
        return "До формы «${stageNames[st + 1]}»: $need ${days(need)}"
    }

    fun days(n: Int): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m10 == 1 && m100 != 11 -> "день"
            m10 in 2..4 && m100 !in 12..14 -> "дня"
            else -> "дней"
        }
    }
}
