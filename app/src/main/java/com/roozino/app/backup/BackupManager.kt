package com.roozino.app.backup

import android.content.Context
import com.roozino.app.data.AppDatabase
import com.roozino.app.data.Event
import com.roozino.app.data.Note
import com.roozino.app.data.Reminder
import com.roozino.app.data.SettingEntry
import com.roozino.app.data.Task
import com.roozino.app.notif.AlarmScheduler
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val app: String = "roozino",
    val version: Int = 1,
    val exportedAt: Long,
    val tasks: List<Task> = emptyList(),
    val notes: List<Note> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val events: List<Event> = emptyList(),
    val settings: List<SettingEntry> = emptyList()
)

object BackupManager {

    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    suspend fun export(db: AppDatabase): String {
        val data = BackupFile(
            exportedAt = System.currentTimeMillis(),
            tasks = db.taskDao().all(),
            notes = db.noteDao().all(),
            reminders = db.reminderDao().all(),
            events = db.eventDao().all(),
            settings = db.settingsDao().all()
        )
        return json.encodeToString(data)
    }

    /**
     * @param replace true = همه داده‌های فعلی پاک و جایگزین می‌شود
     *                false = به داده‌های فعلی «اضافه» می‌شود (با شناسه‌های جدید تا دیتای موجود خراب نشود)
     * @return تعداد آیتم‌های بازیابی‌شده
     */
    suspend fun import(db: AppDatabase, context: Context, text: String, replace: Boolean): Int {
        val data = try {
            json.decodeFromString<BackupFile>(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("فایل پشتیبان معتبر نیست")
        }
        if (data.app != "roozino") throw IllegalArgumentException("این فایل پشتیبان روزینو نیست")

        if (replace) {
            db.reminderDao().deleteAll()
            db.eventDao().deleteAll()
            db.taskDao().deleteAll()
            db.noteDao().deleteAll()
            data.tasks.forEach { db.taskDao().upsert(it) }
            data.notes.forEach { db.noteDao().upsert(it) }
            data.events.forEach { db.eventDao().upsert(it) }
            data.reminders.forEach { db.reminderDao().upsert(it) }
        } else {
            data.tasks.forEach { db.taskDao().upsert(it.copy(id = 0)) }
            data.notes.forEach { db.noteDao().upsert(it.copy(id = 0)) }
            data.events.forEach { db.eventDao().upsert(it.copy(id = 0)) }
            data.reminders.forEach { db.reminderDao().upsert(it.copy(id = 0, taskRef = null)) }
        }
        AlarmScheduler.rescheduleAll(context)
        return data.tasks.size + data.notes.size + data.events.size + data.reminders.size
    }
}
