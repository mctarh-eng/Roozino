package com.roozino.app.notif

import com.roozino.app.data.Reminder
import com.roozino.app.util.TimeMath

/**
 * محاسبه زمان اجرای بعدی یک یادآوری:
 * - اگر زمان تنظیم‌شده در آینده است همان برگردانده می‌شود
 * - برای تکرارها، اولین رخدادِ بعد از `after` محاسبه می‌شود
 */
object NextFire {

    fun computeNext(r: Reminder, after: Long): Long? {
        if (!r.enabled || r.deleted) return null
        val t0 = r.timeMillis
        if (t0 > after) return t0
        return when (r.repeatType) {
            1 -> {
                val k = (after - t0) / TimeMath.DAY_MILLIS + 1
                t0 + k * TimeMath.DAY_MILLIS
            }
            2 -> {
                val k = (after - t0) / (7 * TimeMath.DAY_MILLIS) + 1
                t0 + k * (7 * TimeMath.DAY_MILLIS)
            }
            3 -> {
                var x = TimeMath.addMonths(t0, 1)
                var guard = 0
                while (x <= after && guard < 600) {
                    x = TimeMath.addMonths(x, 1)
                    guard++
                }
                if (x > after) x else null
            }
            else -> null
        }
    }
}
