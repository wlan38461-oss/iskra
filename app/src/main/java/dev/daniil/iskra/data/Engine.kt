package dev.daniil.iskra.data

import dev.daniil.iskra.pet.Mood
import java.time.LocalDate
import java.time.LocalDateTime

/** All the rules, as pure functions on [IskraState]. */
object Engine {
    const val MAX_FREEZES = 3

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

    /**
     * Day change: clears today's ticks, lets embers cover missed days right before today,
     * and hands out a new ember for every 7 days of a streak.
     */
    fun rollover(s: IskraState, today: LocalDate): Pair<IskraState, List<String>> {
        var st = s
        val notes = mutableListOf<String>()
        val key = today.toString()
        if (st.day != key) st = st.copy(day = key, doneIds = emptySet())
        val yesterday = today.minusDays(1)

        var eff = st.effective
        if (yesterday.toString() !in eff && st.freezes > 0 && eff.isNotEmpty()) {
            var d = yesterday
            val gap = mutableListOf<String>()
            while (d.toString() !in eff && gap.size < st.freezes) {
                gap += d.toString()
                d = d.minusDays(1)
            }
            if (d.toString() in eff && gap.isNotEmpty()) {
                st = st.copy(
                    frozen = st.frozen + gap,
                    freezes = st.freezes - gap.size,
                    achievements = st.achievements + "ember_saved",
                )
                notes += if (gap.size == 1) "🪨 Уголёк сгорел и спас твою серию за вчера"
                else "🪨 Угольки (${gap.size}) спасли твою серию"
                eff = st.effective
            }
        }

        val run = streakEnding(eff, yesterday)
        var milestone = st.milestone
        var freezes = st.freezes
        if (run < milestone) milestone = (run / 7) * 7
        var got = 0
        while (run >= milestone + 7) {
            milestone += 7
            if (freezes < MAX_FREEZES) {
                freezes++
                got++
            }
        }
        if (got > 0) notes += "🪨 Новый уголёк за $milestone дней серии! Он сохранит серию, если пропустишь день"
        st = st.copy(milestone = milestone, freezes = freezes)
        return st to notes
    }

    /** Recomputes today's completion, the record and general achievements. */
    fun settle(s: IskraState, today: LocalDate): IskraState {
        val key = today.toString()
        val complete = s.tasks.isNotEmpty() && s.tasks.all { it.id in s.doneIds }
        var st = s.copy(history = if (complete) s.history + key else s.history - key)
        val streak = streak(st.effective, today)
        st = st.copy(best = maxOf(st.best, streak))
        st = st.copy(achievements = st.achievements + Achievements.general(st, streak))
        return st
    }

    fun toggle(s: IskraState, id: Long, now: LocalDateTime): IskraState {
        if (s.tasks.none { it.id == id }) return s
        val wasDone = id in s.doneIds
        val before = summary(s, now)
        var next = s.copy(
            doneIds = if (wasDone) s.doneIds - id else s.doneIds + id,
            totalDone = if (wasDone) maxOf(0, s.totalDone - 1) else s.totalDone + 1,
        )
        val completes = next.tasks.isNotEmpty() && next.tasks.all { it.id in next.doneIds }
        if (completes && !before.complete) {
            next = next.copy(achievements = next.achievements + Achievements.onDayComplete(s, before, now))
        }
        return settle(next, now.toLocalDate())
    }

    fun add(s: IskraState, task: Task, today: LocalDate): IskraState =
        settle(s.copy(tasks = s.tasks + task), today)

    fun update(s: IskraState, task: Task, today: LocalDate): IskraState =
        settle(s.copy(tasks = s.tasks.map { if (it.id == task.id) task else it }), today)

    fun remove(s: IskraState, id: Long, today: LocalDate): IskraState =
        settle(s.copy(tasks = s.tasks.filter { it.id != id }, doneIds = s.doneIds - id), today)

    fun move(s: IskraState, id: Long, delta: Int): IskraState {
        val i = s.tasks.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in s.tasks.indices) return s
        val list = s.tasks.toMutableList()
        val t = list.removeAt(i)
        list.add(j, t)
        return s.copy(tasks = list)
    }

    fun summary(s: IskraState, now: LocalDateTime): Summary {
        val today = now.toLocalDate()
        val eff = s.effective
        val streak = streak(eff, today)
        val total = s.tasks.size
        val done = s.tasks.count { it.id in s.doneIds }
        val complete = total > 0 && done == total
        val broken = streak == 0 && s.history.isNotEmpty() && !complete
        val mood = when {
            complete -> Mood.HAPPY
            broken -> Mood.SMOLDER
            total > 0 && done == 0 && now.hour >= 18 -> Mood.HUNGRY
            else -> Mood.NORMAL
        }
        val lit = s.history.size
        val form = Growth.form(lit)
        val partial = if (!complete && total > 0) done.toFloat() / total else 0f
        val warmth = if (form == 0) (lit + partial).coerceAtMost(Growth.HATCH.toFloat()) else Growth.HATCH.toFloat()
        val week = (6 downTo 0).map { today.minusDays(it.toLong()) }.map {
            val k = it.toString()
            DayCell(it, if (k in s.history) 1 else if (k in s.frozen) 2 else 0)
        }
        return Summary(
            streak = streak,
            best = maxOf(s.best, streak),
            lit = lit,
            form = form,
            growth = Growth.growth(lit),
            warmth = warmth,
            mood = mood,
            done = done,
            total = total,
            complete = complete,
            week = week,
            freezes = s.freezes,
            totalDone = s.totalDone,
            palette = s.settings.palette,
            petName = s.settings.petName.ifBlank { "Искра" },
        )
    }
}
