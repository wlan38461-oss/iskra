package dev.daniil.iskra.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

data class Task(val id: Long, val title: String, val emoji: String)

data class Settings(
    val petName: String = "Искра",
    val userName: String = "",
    val palette: Int = 0,
    val reminderOn: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val vibration: Boolean = true,
)

data class IskraState(
    val tasks: List<Task> = emptyList(),
    /** ids of tasks done today */
    val doneIds: Set<Long> = emptySet(),
    /** dates (yyyy-MM-dd) when every task was done */
    val history: Set<String> = emptySet(),
    /** dates saved by an ember (streak freeze) */
    val frozen: Set<String> = emptySet(),
    val freezes: Int = 0,
    val milestone: Int = 0,
    val best: Int = 0,
    val totalDone: Int = 0,
    val achievements: Set<String> = emptySet(),
    val settings: Settings = Settings(),
) {
    val effective: Set<String> get() = history + frozen
}

object Store {
    private const val PREFS = "iskra"
    const val MAX_FREEZES = 3

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- read / write ----------

    private fun read(p: SharedPreferences): IskraState {
        val tasks = mutableListOf<Task>()
        val arr = JSONArray(p.getString("tasks", "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            tasks += Task(o.getLong("id"), o.getString("title"), o.optString("emoji", "✨"))
        }
        val done = mutableSetOf<Long>()
        if (p.getString("doneDate", "") == LocalDate.now().toString()) {
            val d = JSONArray(p.getString("doneIds", "[]") ?: "[]")
            for (i in 0 until d.length()) done += d.getLong(i)
        }
        val settings = Settings(
            petName = p.getString("petName", null) ?: "Искра",
            userName = p.getString("userName", null) ?: "",
            palette = p.getInt("palette", 0),
            reminderOn = p.getBoolean("reminderOn", false),
            reminderHour = p.getInt("reminderHour", 20),
            reminderMinute = p.getInt("reminderMinute", 0),
            vibration = p.getBoolean("vibration", true),
        )
        return IskraState(
            tasks = tasks,
            doneIds = done,
            history = p.getStringSet("history", null)?.toSet() ?: emptySet(),
            frozen = p.getStringSet("frozen", null)?.toSet() ?: emptySet(),
            freezes = p.getInt("freezes", 0),
            milestone = p.getInt("milestone", 0),
            best = p.getInt("best", 0),
            totalDone = p.getInt("totalDone", 0),
            achievements = p.getStringSet("achievements", null)?.toSet() ?: emptySet(),
            settings = settings,
        )
    }

    private fun write(ctx: Context, s: IskraState) {
        val arr = JSONArray()
        s.tasks.forEach {
            arr.put(JSONObject().put("id", it.id).put("title", it.title).put("emoji", it.emoji))
        }
        val d = JSONArray()
        s.doneIds.forEach { d.put(it) }
        val st = s.settings
        prefs(ctx).edit()
            .putString("tasks", arr.toString())
            .putString("doneDate", LocalDate.now().toString())
            .putString("doneIds", d.toString())
            .putStringSet("history", HashSet(s.history))
            .putStringSet("frozen", HashSet(s.frozen))
            .putInt("freezes", s.freezes)
            .putInt("milestone", s.milestone)
            .putInt("best", s.best)
            .putInt("totalDone", s.totalDone)
            .putStringSet("achievements", HashSet(s.achievements))
            .putString("petName", st.petName)
            .putString("userName", st.userName)
            .putInt("palette", st.palette)
            .putBoolean("reminderOn", st.reminderOn)
            .putInt("reminderHour", st.reminderHour)
            .putInt("reminderMinute", st.reminderMinute)
            .putBoolean("vibration", st.vibration)
            .apply()
    }

    // ---------- day rollover: embers ----------

    /** Loads state and applies day-rollover rules (embers saving the streak, new embers earned). */
    fun load(ctx: Context): IskraState {
        val p = prefs(ctx)
        val raw = read(p)
        val (fixed, notes) = rollover(raw, LocalDate.now())
        if (fixed != raw || notes.isNotEmpty()) {
            write(ctx, fixed)
            if (notes.isNotEmpty()) {
                val old = p.getString("notices", "") ?: ""
                val joined = (old.split('\n').filter { it.isNotBlank() } + notes).joinToString("\n")
                p.edit().putString("notices", joined).apply()
            }
        }
        return fixed
    }

    /** Messages produced during rollover (shown once in the app). */
    fun takeNotices(ctx: Context): List<String> {
        val p = prefs(ctx)
        val list = (p.getString("notices", "") ?: "").split('\n').filter { it.isNotBlank() }
        if (list.isNotEmpty()) p.edit().remove("notices").apply()
        return list
    }

    private fun rollover(s: IskraState, today: LocalDate): Pair<IskraState, List<String>> {
        var st = s
        val notes = mutableListOf<String>()
        val yesterday = today.minusDays(1)

        // 1) embers fill a gap right before today if the streak was alive before it
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

        // 2) every 7 days of a streak earn an ember
        val run = Logic.streakEnding(eff, yesterday)
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

    // ---------- mutations ----------

    private fun commit(ctx: Context, s: IskraState): IskraState {
        val today = LocalDate.now()
        val key = today.toString()
        val complete = s.tasks.isNotEmpty() && s.tasks.all { it.id in s.doneIds }
        var st = s.copy(history = if (complete) s.history + key else s.history - key)
        val streak = Logic.streak(st.effective, today)
        st = st.copy(best = maxOf(st.best, streak))
        st = st.copy(achievements = st.achievements + Achievements.general(st, streak))
        write(ctx, st)
        return st
    }

    fun addTask(ctx: Context, s: IskraState, title: String, emoji: String): IskraState =
        commit(ctx, s.copy(tasks = s.tasks + Task(System.currentTimeMillis(), title.trim(), emoji)))

    fun updateTask(ctx: Context, s: IskraState, id: Long, title: String, emoji: String): IskraState =
        commit(ctx, s.copy(tasks = s.tasks.map { if (it.id == id) it.copy(title = title.trim(), emoji = emoji) else it }))

    fun removeTask(ctx: Context, s: IskraState, id: Long): IskraState =
        commit(ctx, s.copy(tasks = s.tasks.filter { it.id != id }, doneIds = s.doneIds - id))

    fun toggle(ctx: Context, s: IskraState, id: Long): IskraState {
        val now = LocalDateTime.now()
        val wasDone = id in s.doneIds
        val before = Logic.summary(s, now)
        var next = s.copy(
            doneIds = if (wasDone) s.doneIds - id else s.doneIds + id,
            totalDone = if (wasDone) maxOf(0, s.totalDone - 1) else s.totalDone + 1,
        )
        val completesNow = next.tasks.isNotEmpty() && next.tasks.all { it.id in next.doneIds }
        if (completesNow && !before.complete) {
            next = next.copy(achievements = next.achievements + Achievements.onDayComplete(s, before, now))
        }
        return commit(ctx, next)
    }

    fun updateSettings(ctx: Context, s: IskraState, settings: Settings): IskraState {
        val st = s.copy(settings = settings)
        write(ctx, st)
        return st
    }

    fun resetAll(ctx: Context): IskraState {
        prefs(ctx).edit().clear().apply()
        return IskraState()
    }
}
