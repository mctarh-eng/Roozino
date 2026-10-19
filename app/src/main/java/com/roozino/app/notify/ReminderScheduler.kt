package com.roozino.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * زمان‌بندی یادآوری با AlarmManager.
 *
 * رفتار صادقانه (طبق مستندات اندروید):
 * - اگر مجوز «ساعت دقیق» (SCHEDULE_EXACT_ALARM) موجود باشد → آلارم دقیق
 * - در غیر این صورت → آلارم با پنجره ±۱۰ دقیقه (بدون کرش، فقط کمتر دقیق)
 * - پس از ری‌استارت گوشی، SystemEventsReceiver همه یادآوری‌های آینده را دوباره می‌چیند
 */
object ReminderScheduler {

    private const val WINDOW_MILLIS = 10 * 60 * 1000L

    fun schedule(context: Context, taskId: Long, title: String, atEpochMillis: Long) {
        if (atEpochMillis <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pendingIntent(context, taskId, title)
        try {
            val exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
            if (exactAllowed) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMillis, pi)
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, atEpochMillis, WINDOW_MILLIS, pi)
            }
        } catch (_: SecurityException) {
            // اگر سیستم دقیق بودن را رد کرد، با پنجره بازه‌ای ادامه می‌دهیم
            try {
                am.setWindow(AlarmManager.RTC_WAKEUP, atEpochMillis, WINDOW_MILLIS, pi)
            } catch (_: Exception) {
            }
        }
    }

    fun cancel(context: Context, taskId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.cancel(pendingIntent(context, taskId, ""))
    }

    private fun pendingIntent(context: Context, taskId: Long, title: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("title", title)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
