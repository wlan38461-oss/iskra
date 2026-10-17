package dev.daniil.iskra.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Task(val id: Long, val title: String)

data class IskraState(
    val tasks: List<Task>,
    /** ids of tasks done today */
    val doneIds: Set<Long>,
    /** dates (yyyy-MM-dd) when every task was done */
    val history: Set<String>,
    val best: Int,
)

object Store {
    private const val PREFS = "iskra"

    fun load(ctx: Context): IskraState {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tasks = mutableListOf<Task>()
        val arr = JSONArray(p.getString("tasks", "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            tasks += Task(o.getLong("id"), o.getString("title"))
        }
        val done = mutableSetOf<Long>()
        if (p.getString("doneDate", "") == LocalDate.now().toString()) {
            val d = JSONArray(p.getString("doneIds", "[]") ?: "[]")
            for (i in 0 until d.length()) done += d.getLong(i)
        }
        val history = p.getStringSet("history", null)?.toSet() ?: emptySet()
        return IskraState(tasks, done, history, p.getInt("best", 0))
    }

    private fun save(ctx: Context, s: IskraState): IskraState {
        val today = LocalDate.now()
        val key = today.toString()
        val complete = s.tasks.isNotEmpty() && s.tasks.all { it.id in s.doneIds }
        val history = if (complete) s.history + key else s.history - key
        val streak = Logic.streak(history, today)
        val fixed = s.copy(history = history, best = maxOf(s.best, streak))

        val arr = JSONArray()
        fixed.tasks.forEach { arr.put(JSONObject().put("id", it.id).put("title", it.title)) }
        val d = JSONArray()
        fixed.doneIds.forEach { d.put(it) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("tasks", arr.toString())
            .putString("doneDate", key)
            .putString("doneIds", d.toString())
            .putStringSet("history", HashSet(fixed.history))
            .putInt("best", fixed.best)
            .apply()
        return fixed
    }

    fun addTask(ctx: Context, s: IskraState, title: String): IskraState =
        save(ctx, s.copy(tasks = s.tasks + Task(System.currentTimeMillis(), title.trim())))

    fun removeTask(ctx: Context, s: IskraState, id: Long): IskraState =
        save(ctx, s.copy(tasks = s.tasks.filter { it.id != id }, doneIds = s.doneIds - id))

    fun toggle(ctx: Context, s: IskraState, id: Long): IskraState =
        save(ctx, s.copy(doneIds = if (id in s.doneIds) s.doneIds - id else s.doneIds + id))
}
