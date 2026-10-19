package com.roozino.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * کار — تاریخ‌ها به‌صورت استاندارد (epoch millis) ذخیره می‌شوند؛
 * تبدیل به تقویم جلالی فقط در لایه نمایش انجام می‌شود.
 */
@Serializable
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val note: String? = null,
    val dueMillis: Long? = null,
    val hasTime: Boolean = false,
    val priority: Int = 0,              // 0=کم  1=متوسط  2=زیاد
    val category: String? = null,
    val repeatType: Int = 0,            // 0=بدون  1=روزانه  2=هفتگی  3=ماهانه  4=سالانه
    val reminderEnabled: Boolean = false,
    val isDone: Boolean = false,
    val doneAt: Long? = null,
    val archived: Boolean = false,
    val deleted: Boolean = false,       // سطل بازیافت (حذف نرم)
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = false,
    val category: String? = null,
    val archived: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val timeMillis: Long = 0,
    val repeatType: Int = 0,            // 0=یک‌بار  1=روزانه  2=هفتگی  3=ماهانه
    val enabled: Boolean = true,
    val taskRef: Long? = null,          // اگر یادآوریِ یک کار باشد
    val deleted: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val startMillis: Long = 0,
    val endMillis: Long? = null,
    val hasTime: Boolean = true,
    val noteRef: Long? = null,
    val deleted: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
@Entity(tableName = "settings")
data class SettingEntry(
    @PrimaryKey val key: String = "",
    val value: String = ""
)
