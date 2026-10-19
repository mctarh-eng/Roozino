package com.roozino.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.roozino.app.MainActivity
import com.roozino.app.R

/** ساخت کانال اعلان و نمایش یادآوری */
object NotificationHelper {

    const val CHANNEL_REMINDERS = "reminders"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        }
        nm.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            val r = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            return r == PackageManager.PERMISSION_GRANTED
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.areNotificationsEnabled()
    }

    fun showReminder(context: Context, taskId: Long, title: String) {
        if (!canNotify(context)) return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_reminder_title))
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(taskId.toInt(), notification)
        } catch (_: SecurityException) {
            // کاربر اعلان‌ها را بسته است؛ در تنظیمات صادقانه اعلام می‌شود
        }
    }
}
