package com.roozino.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.roozino.app.data.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** نمایش یادآوری در زمان مقرر */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1L)
        val title = intent.getStringExtra("title") ?: return
        if (taskId == -1L) return
        NotificationHelper.showReminder(context, taskId, title)
    }
}

/**
 * پس از ری‌استارت گوشی، به‌روزرسانی اپ یا تغییر ساعت/منطقه زمانی،
 * همه یادآوری‌های آینده از دیتابیس دوباره زمان‌بندی می‌شوند.
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val relevant = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.TIMEZONE_CHANGED"
        if (!relevant) return

        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DataRepository.init(appContext)
                DataRepository.rescheduleAll()
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }
}
