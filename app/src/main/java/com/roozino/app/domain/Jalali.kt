package com.roozino.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

/** تاریخ شمسی */
data class JalaliDate(val year: Int, val month: Int, val day: Int)

/**
 * تبدیل دقیق تقویم هجری شمسی (جلالی) ↔ میلادی.
 *
 * الگوریتم بر اساس پیاده‌سازی آزموده‌شده jalaali-js (الگوریتم Borkowski)
 * است؛ تاریخ‌ها به‌صورت استاندارد (میلادی/LocalDate) ذخیره می‌شوند و
 * تبدیل شمسی فقط برای «نمایش» در همین لایه انجام می‌شود.
 */
object Jalali {

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private val months = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val weekdayNames = mapOf(
        DayOfWeek.SATURDAY to "شنبه",
        DayOfWeek.SUNDAY to "یکشنبه",
        DayOfWeek.MONDAY to "دوشنبه",
        DayOfWeek.TUESDAY to "سه‌شنبه",
        DayOfWeek.WEDNESDAY to "چهارشنبه",
        DayOfWeek.THURSDAY to "پنجشنبه",
        DayOfWeek.FRIDAY to "جمعه"
    )

    /** هدر ستون‌های تقویم؛ هفته ایرانی از شنبه شروع می‌شود */
    val weekdayHeaders = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    private fun div(a: Int, b: Int): Int = a / b
    private fun mod(a: Int, b: Int): Int = a - (a / b) * b

    /** (leap, gy, march) برای سال جلالی داده‌شده */
    private fun jalCal(jy: Int): Triple<Int, Int, Int> {
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
        return Triple(leap, gy, march)
    }

    /** روز ژولین از تاریخ میلادی */
    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
            div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
        d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    /** تاریخ میلادی از روز ژولین — (gy, gm, gd) */
    private fun d2g(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j += div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return Triple(gy, gm, gd)
    }

    /** میلادی → شمسی */
    fun fromGregorian(date: LocalDate): JalaliDate {
        val jdn = g2d(date.year, date.monthValue, date.dayOfMonth)
        val gy = d2g(jdn).first
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(r.second, 3, r.third)
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                val jm = 1 + div(k, 31)
                val jd = mod(k, 31) + 1
                return JalaliDate(jy, jm, jd)
            } else {
                k -= 186
            }
        } else {
            jy -= 1
            k += 179
            if (r.first == 1) k += 1
        }
        val jm = 7 + div(k, 30)
        val jd = mod(k, 30) + 1
        return JalaliDate(jy, jm, jd)
    }

    /** شمسی → میلادی */
    fun toGregorian(jy: Int, jm: Int, jd: Int): LocalDate {
        val r = jalCal(jy)
        val jdn = g2d(r.second, 3, r.third) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
        val g = d2g(jdn)
        return LocalDate.of(g.first, g.second, g.third)
    }

    /** آیا سال شمسی کبیسه است؟ */
    fun isLeapYear(jy: Int): Boolean = jalCal(jy).first == 0

    /** طول ماه شمسی (روز) */
    fun monthLength(jy: Int, jm: Int): Int {
        if (jm <= 6) return 31
        if (jm <= 11) return 30
        return if (isLeapYear(jy)) 30 else 29
    }

    /** ایندکس ستون روز اول ماه در هفته ایرانی (۰ = شنبه) */
    fun firstWeekdayOffset(jy: Int, jm: Int): Int {
        val g = toGregorian(jy, jm, 1)
        return (g.dayOfWeek.value + 1) % 7
    }

    fun monthName(m: Int): String = months.getOrElse(m - 1) { "" }

    fun weekdayName(d: LocalDate): String = weekdayNames[d.dayOfWeek] ?: ""

    fun today(): JalaliDate = fromGregorian(LocalDate.now())

    // ---------- قالب‌بندی نمایشی ----------

    fun toPersianDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            if (c in '0'..'9') sb.append('\u06F0' + (c - '0')) else sb.append(c)
        }
        return sb.toString()
    }

    /** «۱۵ دی ۱۴۰۳» */
    fun format(j: JalaliDate): String =
        "${toPersianDigits(j.day.toString())} ${monthName(j.month)} ${toPersianDigits(j.year.toString())}"

    /** «دوشنبه ۱۵ دی ۱۴۰۳» */
    fun formatFull(d: LocalDate): String {
        val j = fromGregorian(d)
        return "${weekdayName(d)} ${format(j)}"
    }

    /** «۱۵ دی» */
    fun formatShort(d: LocalDate): String {
        val j = fromGregorian(d)
        return "${toPersianDigits(j.day.toString())} ${monthName(j.month)}"
    }
}
