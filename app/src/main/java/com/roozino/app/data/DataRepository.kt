package com.roozino.app.data

import android.content.Context
import androidx.room.withTransaction
import com.roozino.app.domain.PersianText
import com.roozino.app.notify.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * لایه دسترسی داده برای UI — همه ویوها از همین‌جا می‌خوانند.
 * داده‌ها کاملاً محلی‌اند (Room / SQLite).
 */
object DataRepository {

    private lateinit var db: RoozinoDatabase
    private var appContext: Context? = null

    fun init(context: Context) {
        if (!::db.isInitialized) {
            db = RoozinoDatabase.get(context)
            appContext = context.applicationContext
        }
    }

    private fun ctx(): Context = appContext ?: throw IllegalStateException("DataRepository.init not called")

    // ---------- خواندن (Flow برای Compose) ----------

    fun tasksForDate(date: String): Flow<List<TaskEntity>> = db.taskDao().tasksForDate(date)
    fun activeTasks(): Flow<List<TaskEntity>> = db.taskDao().activeTasks()
    fun doneTasks(): Flow<List<TaskEntity>> = db.taskDao().doneTasks()
    fun allNotes(): Flow<List<NoteEntity>> = db.noteDao().allNotes()
    fun recentNotes(limit: Int = 3): Flow<List<NoteEntity>> = db.noteDao().recent(limit)

    suspend fun tasksInRange(fromIso: String, toIso: String): List<TaskEntity> =
        db.taskDao().tasksInRange(fromIso, toIso)

    suspend fun notesCreatedBetween(from: Long, to: Long): List<NoteEntity> =
        db.noteDao().createdBetween(from, to)

    suspend fun allTasks(): List<TaskEntity> = db.taskDao().all()
    suspend fun allNotes(): List<NoteEntity> = db.noteDao().all()

    // ---------- کارها ----------

    /** ثبت کار از ورودی سریع یا فرم؛ اگر تاریخ+ساعت+یادآوری باشد، آلارم زمان‌بندی می‌شود */
    suspend fun addTask(
        title: String,
        date: LocalDate? = null,
        time: LocalTime? = null,
        withReminder: Boolean = false
    ): Long {
        val trimmed = title.trim()
        val reminderAt = if (withReminder && date != null && time != null) {
            date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else null
        val entity = TaskEntity(
            title = trimmed,
            dueDate = date?.toString(),
            dueTime = time?.toString(),
            reminderAt = reminderAt,
            searchNorm = PersianText.normalize(trimmed)
        )
        val id = db.taskDao().insert(entity)
        val r = entity.reminderAt
        if (r != null && r > System.currentTimeMillis()) {
            ReminderScheduler.schedule(ctx(), id, trimmed, r)
        }
        return id
    }

    suspend fun updateTask(task: TaskEntity) {
        val updated = task.copy(
            updatedAt = System.currentTimeMillis(),
            searchNorm = PersianText.normalize(task.title)
        )
        db.taskDao().update(updated)
        val r = updated.reminderAt
        val now = System.currentTimeMillis()
        ReminderScheduler.cancel(ctx(), updated.id)
        if (r != null && !updated.isDone && r > now) {
            ReminderScheduler.schedule(ctx(), updated.id, updated.title, r)
        }
    }

    suspend fun setTaskDone(task: TaskEntity, done: Boolean) {
        db.taskDao().setDone(task.id, done, System.currentTimeMillis())
        val r = task.reminderAt
        if (r != null) {
            if (done) {
                ReminderScheduler.cancel(ctx(), task.id)
            } else if (r > System.currentTimeMillis()) {
                ReminderScheduler.schedule(ctx(), task.id, task.title, r)
            }
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        val r = task.reminderAt
        if (r != null) ReminderScheduler.cancel(ctx(), task.id)
        db.taskDao().delete(task.id)
    }

    // ---------- یادداشت‌ها ----------

    suspend fun addNote(title: String, content: String): Long {
        val entity = NoteEntity(
            title = title.trim(),
            content = content.trim(),
            searchNorm = PersianText.normalize("$title $content")
        )
        return db.noteDao().insert(entity)
    }

    suspend fun updateNote(note: NoteEntity) {
        val updated = note.copy(
            updatedAt = System.currentTimeMillis(),
            searchNorm = PersianText.normalize("${note.title} ${note.content}")
        )
        db.noteDao().update(updated)
    }

    suspend fun deleteNote(note: NoteEntity) = db.noteDao().delete(note.id)

    // ---------- جست‌وجو (نرمال‌سازی فارسی) ----------

    suspend fun searchTasks(query: String): List<TaskEntity> =
        db.taskDao().search(likePattern(query))

    suspend fun searchNotes(query: String): List<NoteEntity> =
        db.noteDao().search(likePattern(query))

    private fun likePattern(query: String): String =
        PersianText.normalize(query)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    // ---------- بازیابی / زمان‌بندی مجدد ----------

    /** بعد از ری‌استارت گوشی یا تغییر ساعت/منطقه زمانی، همه یادآوری‌های آینده دوباره تنظیم می‌شوند */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        db.taskDao().futureReminders(now).forEach { t ->
            val r = t.reminderAt
            if (r != null) ReminderScheduler.schedule(ctx(), t.id, t.title, r)
        }
    }

    /** بازیابی پشتیبان: جایگزینی کامل + زمان‌بندی مجدد یادآوری‌ها */
    suspend fun replaceAll(tasks: List<TaskEntity>, notes: List<NoteEntity>) {
        val scheduled = mutableListOf<Pair<Long, TaskEntity>>()
        db.withTransaction {
            db.taskDao().deleteAll()
            db.noteDao().deleteAll()
            val ids = db.taskDao().insertAll(tasks)
            scheduled += ids.zip(tasks)
            db.noteDao().insertAll(notes)
        }
        val now = System.currentTimeMillis()
        scheduled.forEach { (id, t) ->
            val r = t.reminderAt
            if (r != null && !t.isDone && r > now) {
                ReminderScheduler.schedule(ctx(), id, t.title, r)
            }
        }
    }
}
