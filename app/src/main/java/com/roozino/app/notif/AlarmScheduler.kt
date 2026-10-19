package com.roozino.app.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.roozino.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * زمان‌بندی دقیق یادآوری‌ها با AlarmManager.
 * نکته صادقانه: هیچ سیستمی نمی‌تواند تحویل اعلان را ۱۰۰٪ تضمین کند
 * (بهینه‌ساز باتری برخی گوشی‌ها می‌تواند آن را به تعویق بیندازد) —
 * در صفحه یادآوری‌ها راهنمای غیرفعال‌کردن بهینه‌سازی باتری نمایش داده می‌شود.
 */
object AlarmScheduler {

    const val EXTRA_ID = "reminder_id"
    private const val REQUEST_BASE = 100_000

    private fun pendingIntent(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_ID, reminderId)
        return PendingIntent.getBroadcast(
            context,
            (REQUEST_BASE + reminderId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, reminderId: Long, timeMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, reminderId)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pi)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, reminderId))
    }

    /** زمان‌بندی مجدد همه یادآوری‌های فعال (بعد از بوت، تغییر ساعت/منطقه زمانی و باز شدن برنامه) */
    fun rescheduleAll(context: Context) {
        val db = AppDatabase.get(context)
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            val reminders = db.reminderDao().all().filter { it.enabled && !it.deleted }
            reminders.forEach { r ->
                val next = NextFire.computeNext(r, now)
                if (next != null) schedule(context, r.id, next)
            }
        }
    }
}
