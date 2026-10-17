package dev.daniil.iskra.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.updateAll
import dev.daniil.iskra.data.Achievements
import dev.daniil.iskra.data.IskraState
import dev.daniil.iskra.data.Logic
import dev.daniil.iskra.data.Settings
import dev.daniil.iskra.data.Store
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.reminder.Reminder
import dev.daniil.iskra.widget.IskraWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class Banner(val id: Int, val text: String)

class AppModel(private val ctx: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var bannerSeq = 0

    var state by mutableStateOf(Store.load(ctx))
        private set
    var now by mutableStateOf(LocalDateTime.now())
        private set

    /** increments on every fed task (pet reaction) */
    var feedTick by mutableIntStateOf(0)
        private set

    /** increments when a day gets completed (ember burst) */
    var burstTick by mutableIntStateOf(0)
        private set

    val banners = mutableStateListOf<Banner>()

    val summary: Summary get() = Logic.summary(state, now)

    fun banner(text: String) {
        banners += Banner(++bannerSeq, text)
    }

    fun reload() {
        state = Store.load(ctx)
        now = LocalDateTime.now()
        Store.takeNotices(ctx).forEach { banner(it) }
        Reminder.schedule(ctx, state.settings)
        refreshWidget()
    }

    private fun set(s: IskraState) {
        state = s
        now = LocalDateTime.now()
        refreshWidget()
    }

    private fun refreshWidget() {
        scope.launch { runCatching { IskraWidget().updateAll(ctx) } }
    }

    fun toggle(id: Long) {
        val before = summary
        val prevAch = state.achievements
        val wasDone = id in state.doneIds
        set(Store.toggle(ctx, state, id))
        val after = summary
        if (!wasDone) feedTick++
        if (after.complete && !before.complete) {
            burstTick++
            banner(
                if (after.stage > before.stage) "✨ Эволюция! ${after.petName} теперь — ${Logic.stageNames[after.stage]}"
                else "🔥 День засчитан! Серия: ${after.streak} ${Logic.days(after.streak)}",
            )
        }
        (state.achievements - prevAch).forEach { id2 ->
            Achievements.byId(id2)?.let { banner("🏆 Достижение: ${it.emoji} ${it.title}") }
        }
    }

    fun addTask(title: String, emoji: String) = set(Store.addTask(ctx, state, title, emoji))
    fun updateTask(id: Long, title: String, emoji: String) = set(Store.updateTask(ctx, state, id, title, emoji))
    fun removeTask(id: Long) = set(Store.removeTask(ctx, state, id))

    fun updateSettings(transform: (Settings) -> Settings) {
        set(Store.updateSettings(ctx, state, transform(state.settings)))
        Reminder.schedule(ctx, state.settings)
    }

    fun resetAll() {
        set(Store.resetAll(ctx))
        Reminder.schedule(ctx, state.settings)
    }
}
