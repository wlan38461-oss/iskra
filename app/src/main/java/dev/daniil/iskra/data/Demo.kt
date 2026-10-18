package dev.daniil.iskra.data

import java.time.LocalDate

/** Sample data for screenshots and widget previews. Never saved unless asked. */
object Demo {
    fun state(
        today: LocalDate,
        lit: Int,
        done: Int,
        count: Int = 6,
        palette: Int = 0,
        userName: String = "Даниил",
        hatchSeen: Boolean = true,
        broken: Boolean = false,
    ): IskraState {
        val picks = listOf(0, 1, 2, 3, 4, 9, 13, 5).map { Texts.suggestions[it] }
        val n = count.coerceIn(0, picks.size)
        val tasks = (0 until n).map { i ->
            val (e, t) = picks[i]
            Task(i + 1L, t, e, remind = when (i) { 0 -> 9 * 60; 2 -> 21 * 60; else -> -1 })
        }
        val complete = n > 0 && done >= n
        val lastDay = if (complete) today else today.minusDays(if (broken) 2 else 1)
        val history = (0 until lit).map { lastDay.minusDays(it.toLong()).toString() }.toSet()
        var s = IskraState(
            day = today.toString(),
            tasks = tasks,
            doneIds = tasks.take(done.coerceIn(0, n)).map { it.id }.toSet(),
            history = history,
            freezes = minOf(lit / 7, Engine.MAX_FREEZES),
            milestone = (lit / 7) * 7,
            best = lit,
            totalDone = lit * n + done,
            settings = Settings(petName = "Искра", userName = userName, palette = palette),
            hatchSeen = hatchSeen,
        )
        s = s.copy(achievements = Achievements.general(s, lit) + if (lit > 4) setOf("early", "big_day") else emptySet())
        return s
    }
}
