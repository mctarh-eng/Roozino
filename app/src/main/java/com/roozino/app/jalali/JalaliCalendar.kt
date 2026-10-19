package com.roozino.app.jalali

import java.util.Calendar

/**
 * تبدیل دقیق تقویم جلالی ↔ میلادی (الگوریتم استاندارد Borkowski / jalaali-js)
 * تاریخ استاندارد داخلی = epoch millis؛ این کلاس فقط برای نمایش است.
 */
object JalaliCalendar {

    val MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
    val WEEKDAYS = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
    val WEEKDAYS_SHORT = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private data class JalCalResult(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalCalResult {
        val bl = breaks.size
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until bl) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
        var leap = mod(mod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return JalCalResult(leap, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
                div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
        d -= div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    private fun d2g(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j += div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return Triple(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
    }

    private fun d2j(jdn: Int): Triple<Int, Int, Int> {
        val gy = d2g(jdn).first
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(r.gy, 3, r.march)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                return Triple(jy, 1 + div(k, 31), mod(k, 31) + 1)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        return Triple(jy, 7 + div(k, 30), mod(k, 30) + 1)
    }

    private fun div(a: Int, b: Int): Int = a / b
    private fun mod(a: Int, b: Int): Int = a % b

    // ---------- API عمومی ----------

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> = d2j(g2d(gy, gm, gd))

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> = d2g(j2d(jy, jm, jd))

    fun isJalaliLeap(jy: Int): Boolean = jalCal(jy).leap == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        isJalaliLeap(jy) -> 30
        else -> 29
    }

    fun today(): Triple<Int, Int, Int> = fromMillis(System.currentTimeMillis())

    fun fromMillis(millis: Long): Triple<Int, Int, Int> {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return gregorianToJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    fun jalaliToMillis(jy: Int, jm: Int, jd: Int, hour: Int = 0, minute: Int = 0): Long {
        val g = jalaliToGregorian(jy, jm, jd)
        val c = Calendar.getInstance()
        c.clear()
        c.set(g.first, g.second - 1, g.third, hour, minute, 0)
        return c.timeInMillis
    }

    /** شاخص روز هفته: 0=شنبه … 6=جمعه */
    fun weekdayIndex(millis: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return c.get(Calendar.DAY_OF_WEEK) % 7
    }

    fun weekdayName(millis: Long): String = WEEKDAYS[weekdayIndex(millis)]

    fun nextMonth(jy: Int, jm: Int): Pair<Int, Int> = if (jm == 12) Pair(jy + 1, 1) else Pair(jy, jm + 1)

    fun prevMonth(jy: Int, jm: Int): Pair<Int, Int> = if (jm == 1) Pair(jy - 1, 12) else Pair(jy, jm - 1)
}
