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
import androidx.glance.appwidget.updateAll
import dev.daniil.iskra.MainActivity
import dev.daniil.iskra.R
import dev.daniil.iskra.data.Logic
import dev.daniil.iskra.data.Settings
import dev.daniil.iskra.data.Store
import dev.daniil.iskra.widget.IskraWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

object Reminder {
    const val ACTION = "dev.daniil.iskra.REMIND"
    private const val CHANNEL = "iskra_reminder"

    private fun pending(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, 7,
            Intent(ctx, ReminderReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun schedule(ctx: Context, s: Settings) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(ctx)
        am.cancel(pi)
        if (!s.reminderOn) return
        val now = LocalDateTime.now()
        var t = now.toLocalDate().atTime(s.reminderHour, s.reminderMinute)
        if (!t.isAfter(now)) t = t.plusDays(1)
        val ms = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi)
    }

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Напоминания", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Вечернее напоминание покормить питомца"
                },
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun notify(ctx: Context, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(0xFFFF8C2A.toInt())
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            ctx.getSystemService(NotificationManager::class.java)?.notify(1, n)
        } catch (_: SecurityException) {
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val s = Store.load(ctx)
        if (intent.action == Reminder.ACTION) {
            val sm = Logic.summary(s)
            if (sm.total > 0 && !sm.complete) {
                val left = sm.total - sm.done
                val title = "${sm.petName} ждёт поленьев 🔥"
                val text = buildString {
                    append("Осталось $left ${Logic.plural(left, "дело", "дела", "дел")} — и день засчитан.")
                    if (sm.streak > 0) append(" Не дай серии в ${sm.streak} ${Logic.days(sm.streak)} сгореть!")
                }
                Reminder.notify(ctx, title, text)
            }
        }
        Reminder.schedule(ctx, s.settings)
        val pr = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                IskraWidget().updateAll(ctx)
            } catch (_: Throwable) {
            } finally {
                pr.finish()
            }
        }
    }
}
