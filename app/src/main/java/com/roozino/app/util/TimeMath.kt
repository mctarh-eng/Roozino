package com.roozino.app.util

import java.util.Calendar

object TimeMath {
    const val DAY_MILLIS: Long = 86_400_000L

    fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun addMonths(millis: Long, months: Int): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.add(Calendar.MONTH, months)
        return c.timeInMillis
    }
}

/** محاسبه دفعات بعدی تکرار کارها */
object TaskRepeat {
    fun next(millis: Long, repeatType: Int): Long? = when (repeatType) {
        1 -> millis + TimeMath.DAY_MILLIS
        2 -> millis + 7 * TimeMath.DAY_MILLIS
        3 -> TimeMath.addMonths(millis, 1)
        4 -> TimeMath.addMonths(millis, 12)
        else -> null
    }
}
