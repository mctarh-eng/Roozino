package com.roozino.app.domain

import java.time.LocalDate
import java.time.LocalTime

/** نتیجه تحلیل ورودی سریع */
data class QuickParse(
    val text: String,
    val date: LocalDate?,
    val time: LocalTime?
)

/**
 * ابزارهای متن فارسی:
 * - نرمال‌سازی برای جست‌وجو (ی/ي، ك/ک، نیم‌فاصله، اعراب، اعداد فارسی/عربی)
 * - تحلیل جمله ساده برای ثبت سریع (فردا/امروز/پس‌فردا + ساعت)
 */
object PersianText {

    /** تبدیل ارقام فارسی (۰-۹) و عربی (٠-٩) به لاتین */
    fun toLatinDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            when (c) {
                in '\u06F0'..'\u06F9' -> sb.append('0' + (c - '\u06F0'))
                in '\u0660'..'\u0669' -> sb.append('0' + (c - '\u0660'))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * نرمال‌سازی برای ایندکس جست‌وجو.
     * هر دو «متن ذخیره‌شده» و «عبارت جست‌وجو» با همین تابع نرمال می‌شوند
     * تا ي/ی، ك/ک، نیم‌فاصله و اعراب باعث از دست رفتن نتیجه نشوند.
     */
    fun normalize(input: String): String {
        var s = input
            .replace('\u064A', '\u06CC') // ي → ی
            .replace('\u0643', '\u06A9') // ك → ک
            .replace('\u0640', ' ')      // کشیده (ـ)
            .replace("\u200C", "")       // نیم‌فاصله
            .replace("\u200F", "")
            .replace("\u200E", "")
        for (c in '\u064B'..'\u065F') s = s.replace(c.toString(), "") // اعراب
        s = toLatinDigits(s)
        s = s.lowercase()
        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }

    /**
     * تحلیل ورودی یک‌خطی:
     * - «امروز»، «فردا»، «پس‌فردا» → تاریخ
     * - «۱۸:۳۰» یا «ساعت ۵» → ساعت («عصر» به ساعت اضافه می‌شود)
     * متن پاک‌شده (بدون کلمات زمان) برمی‌گردد.
     */
    fun parseQuickEntry(raw: String, today: LocalDate): QuickParse {
        var text = raw.trim()
        if (text.isEmpty()) return QuickParse("", null, null)

        val latin = toLatinDigits(text) // طول رشته با متن اصلی یکسان است؛ ایندکس‌ها هم‌راستا هستند
        var date: LocalDate? = null
        var time: LocalTime? = null

        val timeMatch = Regex("(\\d{1,2}):(\\d{2})").find(latin)
        val hourOnly = if (timeMatch == null) Regex("ساعت\\s*(\\d{1,2})").find(latin) else null
        val m = timeMatch ?: hourOnly
        if (m != null) {
            val h = m.groupValues[1].toIntOrNull() ?: -1
            val mi = if (timeMatch != null) (m.groupValues[2].toIntOrNull() ?: -1) else 0
            var hh = h
            if (hh in 0..23 && mi in 0..59) {
                if (hh in 1..11 && latin.contains("عصر")) hh += 12
                time = LocalTime.of(hh, mi)
                text = text.removeRange(m.range)
            }
        }

        date = when {
            Regex("پس\\s*فردا|پسفردا").containsMatchIn(text) -> {
                text = text.replace(Regex("پس\\s*فردا|پسفردا"), " ")
                today.plusDays(2)
            }
            text.contains("فردا") -> {
                text = text.replace("فردا", " ")
                today.plusDays(1)
            }
            text.contains("امروز") -> {
                text = text.replace("امروز", " ")
                today
            }
            else -> null
        }

        if (time != null) text = text.replace("ساعت", " ")
        text = text.replace(Regex("\\s+"), " ").trim()
        return QuickParse(text, date, time)
    }
}
