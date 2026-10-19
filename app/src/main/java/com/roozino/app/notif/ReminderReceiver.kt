package com.roozino.app.notif

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.roozino.app.MainActivity
import com.roozino.app.R
import com.roozino.app.RoozinoApp
import com.roozino.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * وقتی آلارم می‌رسد: اعلان با دو دکمه «انجام شد» و «۱۰ دقیقه دیگر» نشان می‌دهد،
 * و اگر یادآوری تکرارشونده باشد، دفعات بعدی را دوباره زمان‌بندی می‌کند.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        if (id <= 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handle(context, id)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, id: Long) {
        val db = AppDatabase.get(context)
        val r = db.reminderDao().byId(id) ?: return
        val now = System.currentTimeMillis()
        if (!r.enabled || r.deleted) return

        // آلارم قدیمی که زمانش هنوز نرسیده → فقط زمان‌بندی مجدد
        if (r.timeMillis > now + 60_000L) {
            AlarmScheduler.schedule(context, r.id, r.timeMillis)
            return
        }

        showNotification(context, r)

        val next = NextFire.computeNext(r, now)
        if (next != null) {
            AlarmScheduler.schedule(context, r.id, next)
        } else {
            db.reminderDao().upsert(r.copy(enabled = false, updatedAt = now))
        }
    }

    private fun showNotification(context: Context, r: com.roozino.app.data.Reminder) {
        val completeIntent = PendingIntent.getBroadcast(
            context,
            (200_000 + r.id).toInt(),
            Intent(context, ReminderActionReceiver::class.java)
                .putExtra(ReminderActionReceiver.ACTION, ReminderActionReceiver.ACTION_COMPLETE)
                .putExtra(AlarmScheduler.EXTRA_ID, r.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            (250_000 + r.id).toInt(),
            Intent(context, ReminderActionReceiver::class.java)
                .putExtra(ReminderActionReceiver.ACTION, ReminderActionReceiver.ACTION_SNOOZE)
                .putExtra(AlarmScheduler.EXTRA_ID, r.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            (300_000 + r.id).toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, RoozinoApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_roozino)
            .setContentTitle(r.title)
            .setContentText("یادآوری روزینو")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, "انجام شد", completeIntent)
            .addAction(0, "۱۰ دقیقه دیگر", snoozeIntent)
            .build()

        val granted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (android.os.Build.VERSION.SDK_INT < 33 || granted) {
            try {
                NotificationManagerCompat.from(context).notify(r.id.toInt(), notification)
            } catch (_: SecurityException) {
            }
        }
    }
}
