package com.roozino.app.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * بعد از ری‌استارت گوشی یا تغییر ساعت/منطقه زمانی،
 * همه یادآوری‌های فعال دوباره زمان‌بندی می‌شوند.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> AlarmScheduler.rescheduleAll(context)
        }
    }
}
