package com.roozino.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * کار.
 * ذخیره‌سازی استاندارد و مستقل از تقویم نمایشی:
 * - dueDate: رشته ISO میلادی «yyyy-MM-dd» (تبدیل شمسی فقط در UI)
 * - dueTime: رشته «HH:mm»
 * - reminderAt: زمان دقیق یادآوری به میلی‌ثانیه epoch
 * - searchNorm: متن نرمال‌شده برای جست‌وجوی فارسی
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val reminderAt: Long? = null,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val searchNorm: String = ""
)

/** یادداشت */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val searchNorm: String = ""
)
