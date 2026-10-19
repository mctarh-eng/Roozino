package com.roozino.app.util

import java.util.Locale
import java.util.Calendar
import com.roozino.app.jalali.JalaliCalendar

/** قالب‌بندی تاریخ/ساعت و اعداد فارسی */
object Fa {

    fun toPersianDigits(s: String): String = buildString {
        s.forEach { c -> append(if (c in '0'..'9') ('۰' + (c - '0')) else c) }
    }

    fun toEnglishDigits(s: String): String = buildString {
        s.forEach { c ->
            append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    else -> c
                }
            )
        }
    }

    fun num(n: Int, persian: Boolean): String = if (persian) toPersianDigits(n.toString()) else n.toString()

    fun date(millis: Long, persian: Boolean = true): String {
        val (jy, jm, jd) = JalaliCalendar.fromMillis(millis)
        val s = "$jd ${JalaliCalendar.MONTHS[jm - 1]} $jy"
        return if (persian) toPersianDigits(s) else s
    }

    fun time(millis: Long, persian: Boolean = true): String {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        val s = String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
        return if (persian) toPersianDigits(s) else s
    }

    fun dateTime(millis: Long, persian: Boolean = true): String =
        "${date(millis, persian)} ساعت ${time(millis, persian)}"

    fun relativeDay(millis: Long, persian: Boolean = true): String {
        val diff = TimeMath.startOfDay(millis) - TimeMath.startOfDay(System.currentTimeMillis())
        return when (diff) {
            0L -> "امروز"
            TimeMath.DAY_MILLIS -> "فردا"
            -TimeMath.DAY_MILLIS -> "دیروز"
            else -> date(millis, persian)
        }
    }

    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "صبح بخیر"
        in 12..16 -> "ظهر بخیر"
        in 17..19 -> "عصر بخیر"
        else -> "شب بخیر"
    }
}
