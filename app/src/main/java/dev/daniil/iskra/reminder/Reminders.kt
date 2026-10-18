package dev.daniil.iskra.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.daniil.iskra.MainActivity
import dev.daniil.iskra.R
import dev.daniil.iskra.data.Engine
import dev.daniil.iskra.data.IskraState
import dev.daniil.iskra.data.Repo
import dev.daniil.iskra.data.Summary
import dev.daniil.iskra.data.Task
import dev.daniil.iskra.data.Texts
import dev.daniil.iskra.pet.Palettes
import dev.daniil.iskra.pet.PetDraw
import dev.daniil.iskra.scene.Sky
import dev.daniil.iskra.widget.refreshWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * One exact alarm at a time: it fires at the next due moment (morning, evening,
 * last chance, a task's own time, a snooze or a widget refresh), handles everything
 * due since the previous run and schedules the next one.
 */
object Reminders {
    private const val CH_DAILY = "daily"
    private const val CH_TASKS = "tasks"
    const val ACTION_ALARM = "dev.daniil.iskra.ALARM"
    const val ACTION_DONE = "dev.daniil.iskra.DONE"
    const val ACTION_SNOOZE = "dev.daniil.iskra.SNOOZE"
    const val EXTRA_TASK = "task"
    private const val ID_MORNING = 101
    private const val ID_EVENING = 102
    private const val ID_LAST = 103
    private const val ID_DAY = 104

    fun taskNotifId(id: Long): Int = 1000 + (id % 900_000L).toInt().let { if (it < 0) -it else it }

    data class Ev(val at: LocalDateTime, val kind: String, val task: Long = 0)

    private fun millis(t: LocalDateTime): Long = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private fun fromMillis(ms: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())

    /** Everything due in (from, to]. */
    fun events(s: IskraState, snoozes: List<Pair<Long, Long>>, from: LocalDateTime, to: LocalDateTime): List<Ev> {
        val out = mutableListOf<Ev>()
        var d = from.toLocalDate()
        while (!d.isAfter(to.toLocalDate())) {
            val day = d
            fun add(minute: Int, kind: String, task: Long = 0) {
                val t = day.atStartOfDay().plusMinutes(minute.toLong())
                if (t.isAfter(from) && !t.isAfter(to)) out += Ev(t, kind, task)
            }
            val st = s.settings
            if (st.morningOn) add(st.morningAt, "morning")
            if (st.eveningOn) add(st.eveningAt, "evening")
            if (st.lastOn) add(st.lastAt, "last")
            for (t in s.tasks) if (t.remind >= 0) add(t.remind, "task", t.id)
            for (m in Sky.phaseStarts) add(m, "tick")
            add(0, "tick")
            d = d.plusDays(1)
        }
        for ((id, at) in snoozes) {
            val t = fromMillis(at)
            if (t.isAfter(from) && !t.isAfter(to)) out += Ev(t, "task", id)
        }
        return out.sortedBy { it.at }
    }

    private fun lastRun(ctx: Context, now: LocalDateTime): LocalDateTime {
        val ms = Repo.prefs(ctx).getLong("remLast", 0L)
        if (ms <= 0L) return now
        val t = fromMillis(ms)
        return if (t.isAfter(now.plusMinutes(1))) now else t
    }

    fun schedule(ctx: Context, s: IskraState = Repo.load(ctx)) {
        val now = LocalDateTime.now()
        val from = maxOf(now, lastRun(ctx, now))
        val next = events(s, Repo.snoozes(ctx), from, now.plusDays(2)).firstOrNull() ?: return
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            ctx, 0,
            Intent(ctx, ReminderReceiver::class.java).setAction(ACTION_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ms = millis(next.at)
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
        }
    }

    /** Handles everything due since the last run. Called by the alarm. */
    fun process(ctx: Context) {
        val now = LocalDateTime.now()
        val last = lastRun(ctx, now.minusMinutes(2))
        val from = maxOf(last, now.minusMinutes(90))
        val until = now.plusSeconds(30)
        val s = Repo.load(ctx)
        val sm = Engine.summary(s, now)
        for (e in events(s, Repo.snoozes(ctx), from, until)) handle(ctx, e, s, sm)
        Repo.dropSnoozes(ctx, millis(until))
        Repo.prefs(ctx).edit().putLong("remLast", millis(until)).apply()
        schedule(ctx, s)
    }

    private fun handle(ctx: Context, e: Ev, s: IskraState, sm: Summary) {
        val open = sm.total > 0 && !sm.complete
        when (e.kind) {
            "morning" -> if (open) {
                val emojis = s.tasks.take(6).joinToString(" ") { it.emoji }
                notify(ctx, ID_MORNING, CH_DAILY, Texts.morningTitle(), Texts.morningText(sm, emojis), sm)
            }
            "evening" -> if (open) notify(ctx, ID_EVENING, CH_DAILY, Texts.eveningTitle(sm), Texts.eveningText(sm), sm)
            "last" -> if (open && (sm.streak > 0 || sm.lit > 0)) {
                notify(ctx, ID_LAST, CH_DAILY, Texts.lastTitle(sm), Texts.lastText(sm), sm)
            }
            "task" -> {
                val t = s.tasks.firstOrNull { it.id == e.task } ?: return
                if (t.id !in s.doneIds) notifyTask(ctx, t, sm)
            }
        }
    }

    // ------------------------------------------------------------- notifications

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CH_DAILY) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CH_DAILY, "Утро и вечер", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "План на день, вечернее напоминание и «последний шанс»"
                },
            )
        }
        if (nm.getNotificationChannel(CH_TASKS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CH_TASKS, "Дела", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Напоминания о конкретных делах в их время"
                },
            )
        }
    }

    fun allowed(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun base(ctx: Context, channel: String, title: String, text: String, sm: Summary): NotificationCompat.Builder {
        val pal = Palettes.get(sm.palette)
        return NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(PetDraw.bitmap(192, sm.look, glow = 0.7f))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(pal.accent.toInt())
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
    }

    @SuppressLint("MissingPermission")
    private fun post(ctx: Context, id: Int, b: NotificationCompat.Builder) {
        if (!allowed(ctx)) return
        ensureChannels(ctx)
        try {
            NotificationManagerCompat.from(ctx).notify(id, b.build())
        } catch (_: SecurityException) {
        }
    }

    private fun notify(ctx: Context, id: Int, channel: String, title: String, text: String, sm: Summary) =
        post(ctx, id, base(ctx, channel, title, text, sm))

    private fun action(ctx: Context, action: String, task: Long, code: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx, code,
        Intent(ctx, ActionReceiver::class.java).setAction(action).putExtra(EXTRA_TASK, task),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun notifyTask(ctx: Context, t: Task, sm: Summary) {
        val nid = taskNotifId(t.id)
        val b = base(ctx, CH_TASKS, "${t.emoji} ${t.title}", Texts.taskText(sm.petName), sm)
            .addAction(0, "✓ Готово", action(ctx, ACTION_DONE, t.id, nid * 2))
            .addAction(0, "⏰ Через 30 мин", action(ctx, ACTION_SNOOZE, t.id, nid * 2 + 1))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        post(ctx, nid, b)
    }

    fun notifyDayComplete(ctx: Context, before: Summary, after: Summary) {
        val (title, text) = when {
            !before.hatched && after.hatched -> "🐣 Огонёк вылупился!" to "Открой «Искру» и дай ему имя"
            else -> "🔥 День засчитан!" to "Серия: ${after.streak} ${Texts.days(after.streak)}. ${after.petName} сияет"
        }
        notify(ctx, ID_DAY, CH_DAILY, title, text, after)
    }

    fun cancelTask(ctx: Context, id: Long) = NotificationManagerCompat.from(ctx).cancel(taskNotifId(id))

    fun cancelDaily(ctx: Context) {
        val nm = NotificationManagerCompat.from(ctx)
        nm.cancel(ID_MORNING)
        nm.cancel(ID_EVENING)
        nm.cancel(ID_LAST)
    }

    /** Sample notifications for screenshots. */
    fun demo(ctx: Context, kind: String) {
        val s = Repo.load(ctx)
        val sm = Engine.summary(s, LocalDateTime.now())
        if (kind == "task" || kind == "all") test(ctx)
        if (kind == "evening" || kind == "all") notify(ctx, ID_EVENING, CH_DAILY, Texts.eveningTitle(sm), Texts.eveningText(sm), sm)
        if (kind == "last" || kind == "all") notify(ctx, ID_LAST, CH_DAILY, Texts.lastTitle(sm), Texts.lastText(sm), sm)
        if (kind == "morning" || kind == "all") {
            val emojis = s.tasks.take(6).joinToString(" ") { it.emoji }
            notify(ctx, ID_MORNING, CH_DAILY, Texts.morningTitle(), Texts.morningText(sm, emojis), sm)
        }
    }

    /** A sample notification from the settings screen. */
    fun test(ctx: Context) {
        val s = Repo.load(ctx)
        val sm = Engine.summary(s, LocalDateTime.now())
        val t = s.tasks.firstOrNull() ?: Task(-1, "Выпить стакан воды", "💧")
        notifyTask(ctx, t, sm)
    }

    /** Ticks a task from outside the app (notification button, widget). */
    suspend fun toggleOutside(ctx: Context, id: Long, onlyMarkDone: Boolean) {
        val now = LocalDateTime.now()
        val s0 = Repo.load(ctx)
        if (onlyMarkDone && id in s0.doneIds) {
            cancelTask(ctx, id)
            return
        }
        val before = Engine.summary(s0, now)
        val st = Repo.update(ctx) { Engine.toggle(it, id, now) }
        val after = Engine.summary(st, now)
        cancelTask(ctx, id)
        if (after.complete && !before.complete) {
            cancelDaily(ctx)
            notifyDayComplete(ctx, before, after)
        }
        schedule(ctx, st)
        refreshWidgets(ctx)
    }
}

/** Alarm, boot, time change and app update. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pr = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    Reminders.ACTION_ALARM, "dev.daniil.iskra.REMIND" -> Reminders.process(ctx)
                    else -> Reminders.schedule(ctx)
                }
                refreshWidgets(ctx)
            } catch (_: Throwable) {
            } finally {
                pr.finish()
            }
        }
    }
}

/** «Готово» and «Через 30 мин» buttons on task notifications. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra(Reminders.EXTRA_TASK, Long.MIN_VALUE)
        if (id == Long.MIN_VALUE) return
        val pr = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    Reminders.ACTION_DONE -> if (id >= 0) Reminders.toggleOutside(ctx, id, onlyMarkDone = true) else Reminders.cancelTask(ctx, id)
                    Reminders.ACTION_SNOOZE -> {
                        Repo.addSnooze(ctx, id, System.currentTimeMillis() + 30 * 60 * 1000L)
                        Reminders.cancelTask(ctx, id)
                        Reminders.schedule(ctx)
                    }
                }
            } catch (_: Throwable) {
            } finally {
                pr.finish()
            }
        }
    }
}
