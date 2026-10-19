package com.roozino.app.util

/**
 * نرمال‌سازی متن فارسی برای جست‌وجو:
 * ي/ی  ک/ك  ه/ة  ی/إ/أ  نیم‌فاصله و فاصله‌های اضافی
 */
object PersianNormalizer {

    fun normalize(s: String): String = s
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace('ؤ', 'و')
        .replace('إ', 'ی')
        .replace('أ', 'ی')
        .replace('ٱ', 'ا')
        .replace('ة', 'ه')
        .replace('\u200c', ' ')                     // نیم‌فاصله
        .replace(Regex("[\\u200b-\\u200f\\u202a-\\u202e]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun contains(haystack: String, needle: String): Boolean =
        normalize(haystack).contains(normalize(needle), ignoreCase = true)
}
