package dev.daniil.iskra.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.daniil.iskra.data.Achievements
import dev.daniil.iskra.data.Engine
import dev.daniil.iskra.data.Growth
import dev.daniil.iskra.data.IskraState
import dev.daniil.iskra.data.Repo
import dev.daniil.iskra.data.Settings
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.data.Task
import dev.daniil.iskra.data.Texts
import dev.daniil.iskra.reminder.Reminders
import dev.daniil.iskra.scene.Sky
import dev.daniil.iskra.scene.SkyState
import dev.daniil.iskra.widget.refreshWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class Banner(val id: Int, val text: String)

/** Demo mode for screenshots: fixed time of day, nothing is saved. */
class DemoConfig(val state: IskraState, val minute: Int)

class AppModel(private val ctx: Context, private val demo: DemoConfig?) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var bannerSeq = 0

    var state by mutableStateOf(demo?.state ?: Repo.load(ctx))
        private set
    var now by mutableStateOf(clock())
        private set

    /** pet reaction when a task gets done */
    var feedTick by mutableIntStateOf(0)
        private set

    /** ember burst when the day is complete */
    var burstTick by mutableIntStateOf(0)
        private set

    var showHatch by mutableStateOf(false)

    val banners = mutableStateListOf<Banner>()

    val isDemo: Boolean get() = demo != null

    val summary: Summary get() = Engine.summary(state, now)
    val minute: Int get() = now.hour * 60 + now.minute
    val sky: SkyState get() = Sky.at(minute)

    init {
        if (demo == null && summary.hatched && !state.hatchSeen) showHatch = true
    }

    private fun clock(): LocalDateTime {
        val d = demo ?: return LocalDateTime.now()
        return LocalDate.now().atTime(d.minute / 60, d.minute % 60)
    }

    fun banner(text: String) {
        banners += Banner(++bannerSeq, text)
    }

    fun reload() {
        now = clock()
        if (demo != null) return
        state = Repo.load(ctx)
        Repo.takeNotices(ctx).forEach { banner(it) }
        Reminders.schedule(ctx, state)
        if (summary.hatched && !state.hatchSeen) showHatch = true
        refresh()
    }

    /** Called every half minute: moves the sky and handles midnight. */
    fun tick() {
        val n = clock()
        if (n.toLocalDate() != now.toLocalDate() && demo == null) reload() else now = n
    }

    private fun refresh() {
        if (demo != null) return
        scope.launch(Dispatchers.Default) { refreshWidgets(ctx) }
    }

    private fun commit(s: IskraState) {
        state = s
        now = clock()
        if (demo != null) return
        Repo.save(ctx, s)
        Reminders.schedule(ctx, s)
        refresh()
    }

    fun toggle(id: Long) {
        val before = summary
        val prevAch = state.achievements
        val wasDone = id in state.doneIds
        commit(Engine.toggle(state, id, now))
        val after = summary
        if (!wasDone) {
            feedTick++
            if (demo == null) Reminders.cancelTask(ctx, id)
        }
        if (after.complete && !before.complete) {
            burstTick++
            if (demo == null) Reminders.cancelDaily(ctx)
            when {
                !before.hatched && after.hatched -> scope.launch {
                    delay(900)
                    showHatch = true
                }
                after.form > before.form -> banner("✨ Эволюция! ${after.petName} теперь — ${Texts.formName(after)}")
                after.form == 0 -> {
                    val left = Growth.HATCH - after.lit
                    banner("🥚 Яйцо согрелось! До вылупления $left ${Texts.days(left)}")
                }
                else -> banner("🔥 День засчитан! Серия: ${after.streak} ${Texts.days(after.streak)}")
            }
        }
        (state.achievements - prevAch).forEach { a ->
            Achievements.byId(a)?.let { banner("🏆 Достижение: ${it.emoji} ${it.title}") }
        }
    }

    fun addTask(title: String, emoji: String, remind: Int) =
        commit(Engine.add(state, Task(System.currentTimeMillis(), title.trim(), emoji, remind), now.toLocalDate()))

    fun updateTask(t: Task) = commit(Engine.update(state, t, now.toLocalDate()))

    fun removeTask(id: Long) {
        commit(Engine.remove(state, id, now.toLocalDate()))
        if (demo == null) Reminders.cancelTask(ctx, id)
    }

    fun moveTask(id: Long, delta: Int) = commit(Engine.move(state, id, delta))

    fun updateSettings(f: (Settings) -> Settings) = commit(state.copy(settings = f(state.settings)))

    fun finishHatch(name: String) {
        showHatch = false
        commit(state.copy(hatchSeen = true, settings = state.settings.copy(petName = name.trim().ifBlank { "Искра" })))
        banner("🐣 Привет, ${state.settings.petName}! Корми меня делами каждый день")
    }

    fun resetAll() {
        if (demo == null) Repo.clear(ctx)
        state = Engine.rollover(IskraState(hatchSeen = false), now.toLocalDate()).first
        commit(state)
    }
}
