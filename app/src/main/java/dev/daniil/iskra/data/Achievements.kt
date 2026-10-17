package dev.daniil.iskra.data

import java.time.LocalDateTime

data class Achievement(val id: String, val emoji: String, val title: String, val desc: String)

object Achievements {
    val all = listOf(
        Achievement("first_log", "🪵", "Первое полено", "Выполни первое дело"),
        Achievement("first_day", "🔥", "Разжечь огонь", "Закрой день целиком"),
        Achievement("streak3", "🕯️", "Огонёк", "Серия 3 дня"),
        Achievement("streak7", "🌋", "Неделя пламени", "Серия 7 дней"),
        Achievement("streak14", "🌟", "Феникс", "Серия 14 дней"),
        Achievement("streak30", "☀️", "Вечный огонь", "Серия 30 дней"),
        Achievement("done50", "🪓", "Дровосек", "50 выполненных дел"),
        Achievement("done200", "🌲", "Лесоповал", "200 выполненных дел"),
        Achievement("days30", "📅", "Месяц тепла", "30 засчитанных дней"),
        Achievement("early", "🌅", "Ранняя пташка", "Закрой день до 9:00"),
        Achievement("owl", "🦉", "Ночная сова", "Закрой день после 23:00"),
        Achievement("big_day", "🎪", "Большой костёр", "Закрой день из 7+ дел"),
        Achievement("comeback", "💫", "Из пепла", "Верни огонь после сгоревшей серии"),
        Achievement("ember_saved", "🪨", "Спасён угольком", "Уголёк сохранил твою серию"),
    )

    fun byId(id: String): Achievement? = all.firstOrNull { it.id == id }

    fun general(s: IskraState, streak: Int): Set<String> {
        val a = mutableSetOf<String>()
        val best = maxOf(s.best, streak)
        if (s.totalDone >= 1) a += "first_log"
        if (s.history.isNotEmpty()) a += "first_day"
        if (best >= 3) a += "streak3"
        if (best >= 7) a += "streak7"
        if (best >= 14) a += "streak14"
        if (best >= 30) a += "streak30"
        if (s.totalDone >= 50) a += "done50"
        if (s.totalDone >= 200) a += "done200"
        if (s.history.size >= 30) a += "days30"
        return a
    }

    /** Called when the day becomes complete. [before] is the summary before the last toggle. */
    fun onDayComplete(prev: IskraState, before: Summary, now: LocalDateTime): Set<String> {
        val a = mutableSetOf<String>()
        if (now.hour < 9) a += "early"
        if (now.hour >= 23) a += "owl"
        if (prev.tasks.size >= 7) a += "big_day"
        if (before.streak == 0 && prev.history.isNotEmpty()) a += "comeback"
        return a
    }
}
