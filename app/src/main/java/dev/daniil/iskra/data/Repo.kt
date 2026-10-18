package dev.daniil.iskra.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** SharedPreferences storage. Load applies the day rollover. */
object Repo {
    private const val PREFS = "iskra"

    fun prefs(ctx: Context): SharedPreferences = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun load(ctx: Context): IskraState {
        val p = prefs(ctx)
        val raw = read(p)
        val (st, notes) = Engine.rollover(raw, LocalDate.now())
        if (st != raw) write(p, st)
        if (notes.isNotEmpty()) {
            val old = (p.getString("notices", "") ?: "").split('\n').filter { it.isNotBlank() }
            p.edit().putString("notices", (old + notes).joinToString("\n")).apply()
        }
        return st
    }

    @Synchronized
    fun save(ctx: Context, s: IskraState) = write(prefs(ctx), s)

    /** Load → change → save in one step (widgets, notification buttons). */
    @Synchronized
    fun update(ctx: Context, f: (IskraState) -> IskraState): IskraState {
        val s = f(load(ctx))
        write(prefs(ctx), s)
        return s
    }

    fun takeNotices(ctx: Context): List<String> {
        val p = prefs(ctx)
        val list = (p.getString("notices", "") ?: "").split('\n').filter { it.isNotBlank() }
        if (list.isNotEmpty()) p.edit().remove("notices").apply()
        return list
    }

    fun flag(ctx: Context, key: String): Boolean = prefs(ctx).getBoolean(key, false)
    fun setFlag(ctx: Context, key: String, v: Boolean) = prefs(ctx).edit().putBoolean(key, v).apply()

    /** Minute of the day forced by the screenshot demo, or -1. */
    fun debugMinute(ctx: Context): Int = prefs(ctx).getInt("debugMinute", -1)
    fun setDebugMinute(ctx: Context, m: Int) = prefs(ctx).edit().putInt("debugMinute", m).apply()

    // ------------------------------------------------------------- snoozes

    @Synchronized
    fun snoozes(ctx: Context): List<Pair<Long, Long>> {
        val arr = JSONArray(prefs(ctx).getString("snoozes", "[]") ?: "[]")
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            o.getLong("id") to o.getLong("at")
        }
    }

    @Synchronized
    fun addSnooze(ctx: Context, id: Long, atMillis: Long) {
        val list = snoozes(ctx).filter { it.first != id } + (id to atMillis)
        saveSnoozes(ctx, list)
    }

    @Synchronized
    fun dropSnoozes(ctx: Context, upToMillis: Long) {
        saveSnoozes(ctx, snoozes(ctx).filter { it.second > upToMillis })
    }

    private fun saveSnoozes(ctx: Context, list: List<Pair<Long, Long>>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.first).put("at", it.second)) }
        prefs(ctx).edit().putString("snoozes", arr.toString()).apply()
    }

    // ------------------------------------------------------------- format

    private fun read(p: SharedPreferences): IskraState {
        val tasks = mutableListOf<Task>()
        val arr = JSONArray(p.getString("tasks", "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            tasks += Task(o.getLong("id"), o.getString("title"), o.optString("emoji", "✨"), o.optInt("remind", -1))
        }
        val done = mutableSetOf<Long>()
        val d = JSONArray(p.getString("doneIds", "[]") ?: "[]")
        for (i in 0 until d.length()) done += d.getLong(i)

        val evening = when {
            p.contains("eveningAt") -> p.getInt("eveningAt", 20 * 60)
            p.contains("reminderHour") -> p.getInt("reminderHour", 20) * 60 + p.getInt("reminderMinute", 0)
            else -> 20 * 60
        }
        val settings = Settings(
            petName = p.getString("petName", null) ?: "Искра",
            userName = p.getString("userName", null) ?: "",
            palette = p.getInt("palette", 0),
            vibration = p.getBoolean("vibration", true),
            autoTheme = p.getBoolean("autoTheme", true),
            morningOn = p.getBoolean("morningOn", true),
            morningAt = p.getInt("morningAt", 9 * 60),
            eveningOn = p.getBoolean("eveningOn", true),
            eveningAt = evening,
            lastOn = p.getBoolean("lastOn", true),
            lastAt = p.getInt("lastAt", 22 * 60 + 30),
        )
        return IskraState(
            day = p.getString("doneDate", "") ?: "",
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
            hatchSeen = p.getBoolean("hatchSeen", false),
        )
    }

    private fun write(p: SharedPreferences, s: IskraState) {
        val arr = JSONArray()
        s.tasks.forEach {
            arr.put(JSONObject().put("id", it.id).put("title", it.title).put("emoji", it.emoji).put("remind", it.remind))
        }
        val d = JSONArray()
        s.doneIds.forEach { d.put(it) }
        val st = s.settings
        p.edit()
            .putString("tasks", arr.toString())
            .putString("doneDate", s.day)
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
            .putBoolean("vibration", st.vibration)
            .putBoolean("autoTheme", st.autoTheme)
            .putBoolean("morningOn", st.morningOn)
            .putInt("morningAt", st.morningAt)
            .putBoolean("eveningOn", st.eveningOn)
            .putInt("eveningAt", st.eveningAt)
            .putBoolean("lastOn", st.lastOn)
            .putInt("lastAt", st.lastAt)
            .putBoolean("hatchSeen", s.hatchSeen)
            .apply()
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }
}
