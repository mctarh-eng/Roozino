package com.roozino.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roozino.app.backup.BackupManager
import com.roozino.app.data.Event
import com.roozino.app.data.Note
import com.roozino.app.data.Reminder
import com.roozino.app.data.SettingEntry
import com.roozino.app.data.Task
import com.roozino.app.data.TaskOps
import com.roozino.app.notif.AlarmScheduler
import com.roozino.app.notif.NextFire
import com.roozino.app.util.PersianNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class QuickType { TASK, NOTE, REMINDER }

data class UiSettings(
    val loaded: Boolean = false,
    val themeMode: String = "system",   // system | light | dark
    val persianDigits: Boolean = true,
    val fontScale: Float = 1f,          // 0.85 | 1.0 | 1.2
    val welcomeDone: Boolean = false
)

data class SearchResults(
    val query: String = "",
    val tasks: List<Task> = emptyList(),
    val notes: List<Note> = emptyList(),
    val events: List<Event> = emptyList(),
    val reminders: List<Reminder> = emptyList()
) {
    fun totalCount(): Int = tasks.size + notes.size + events.size + reminders.size
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as RoozinoApp).db
    private val taskDao = db.taskDao()
    private val noteDao = db.noteDao()
    private val reminderDao = db.reminderDao()
    private val eventDao = db.eventDao()
    private val settingsDao = db.settingsDao()
    private val context = app

    val tasks: StateFlow<List<Task>> =
        taskDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notes: StateFlow<List<Note>> =
        noteDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminders: StateFlow<List<Reminder>> =
        reminderDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val events: StateFlow<List<Event>> =
        eventDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<UiSettings> = settingsDao.observe().map { list ->
        val map = list.associate { it.key to it.value }
        UiSettings(
            loaded = true,
            themeMode = map["theme_mode"] ?: "system",
            persianDigits = (map["persian_digits"] ?: "1") == "1",
            fontScale = map["font_scale"]?.toFloatOrNull() ?: 1f,
            welcomeDone = (map["welcome_done"] ?: "0") == "1"
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings())

    fun setSetting(key: String, value: String) = viewModelScope.launch(Dispatchers.IO) {
        settingsDao.upsert(SettingEntry(key = key, value = value))
    }

    /** خودترمیمی: موقع باز شدن برنامه همه یادآوری‌های فعال دوباره زمان‌بندی می‌شوند */
    fun rescheduleAllNow() = viewModelScope.launch(Dispatchers.IO) {
        AlarmScheduler.rescheduleAll(context)
    }

    // ---------------- ثبت سریع ----------------

    fun addQuick(text: String, type: QuickType, dueMillis: Long? = null) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            when (type) {
                QuickType.TASK -> taskDao.upsert(Task(title = t, createdAt = now, updatedAt = now))
                QuickType.NOTE -> {
                    val lines = t.split('\n', limit = 2)
                    noteDao.upsert(
                        Note(
                            title = lines[0].take(80),
                            body = lines.getOrElse(1) { "" },
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
                QuickType.REMINDER -> {
                    val time = dueMillis ?: (now + 60 * 60 * 1000L)
                    val r = Reminder(title = t, timeMillis = time, createdAt = now, updatedAt = now)
                    val id = reminderDao.upsert(r)
                    AlarmScheduler.schedule(context, id, time)
                }
            }
        }
    }

    // ---------------- کارها ----------------

    suspend fun getTask(id: Long): Task? = withContext(Dispatchers.IO) { taskDao.byId(id) }

    fun saveTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val isNew = task.id == 0L
        val base = task.copy(createdAt = if (isNew) now else task.createdAt, updatedAt = now)
        val id = taskDao.upsert(base)
        val saved = if (isNew) base.copy(id = id) else base
        syncTaskReminder(saved, now)
    }

    private suspend fun syncTaskReminder(task: Task, now: Long) {
        val existing = reminderDao.all().firstOrNull { it.taskRef == task.id && !it.deleted }
        if (task.reminderEnabled && task.dueMillis != null && !task.isDone && !task.deleted && !task.archived) {
            val r = (existing ?: Reminder(taskRef = task.id, createdAt = now)).copy(
                title = task.title,
                timeMillis = task.dueMillis!!,
                enabled = true,
                updatedAt = now
            )
            val id = reminderDao.upsert(r)
            val rid = if (r.id != 0L) r.id else id
            val next = NextFire.computeNext(r.copy(id = rid), now)
            if (next != null) AlarmScheduler.schedule(context, rid, next)
            else AlarmScheduler.cancel(context, rid)
        } else if (existing != null && existing.enabled) {
            AlarmScheduler.cancel(context, existing.id)
            reminderDao.upsert(existing.copy(enabled = false, updatedAt = now))
        }
    }

    fun toggleTaskDone(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        TaskOps.completeTask(db, context, task)
    }

    fun reopenTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val t = task.copy(isDone = false, doneAt = null, updatedAt = now)
        taskDao.upsert(t)
        syncTaskReminder(t, now)
    }

    fun archiveTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        val t = task.copy(archived = true, updatedAt = System.currentTimeMillis())
        taskDao.upsert(t); syncTaskReminder(t, System.currentTimeMillis())
    }

    fun unarchiveTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        val t = task.copy(archived = false, updatedAt = System.currentTimeMillis())
        taskDao.upsert(t); syncTaskReminder(t, System.currentTimeMillis())
    }

    fun trashTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        val t = task.copy(deleted = true, updatedAt = System.currentTimeMillis())
        taskDao.upsert(t); syncTaskReminder(t, System.currentTimeMillis())
    }

    fun restoreTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        val t = task.copy(deleted = false, updatedAt = System.currentTimeMillis())
        taskDao.upsert(t); syncTaskReminder(t, System.currentTimeMillis())
    }

    fun purgeTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        reminderDao.all().firstOrNull { it.taskRef == task.id }?.let {
            AlarmScheduler.cancel(context, it.id)
            reminderDao.deleteById(it.id)
        }
        taskDao.deleteById(task.id)
    }

    // ---------------- یادداشت‌ها ----------------

    suspend fun getNote(id: Long): Note? = withContext(Dispatchers.IO) { noteDao.byId(id) }

    /** ذخیره خودکار یادداشت؛ اگر خالی باشد کاری نمی‌کند. id تازه را برمی‌گرداند */
    suspend fun saveNoteLive(id: Long, title: String, body: String, pinned: Boolean): Long =
        withContext(Dispatchers.IO) {
            if (title.isBlank() && body.isBlank()) return@withContext id
            val now = System.currentTimeMillis()
            if (id == 0L) {
                noteDao.upsert(
                    Note(title = title.trim(), body = body, pinned = pinned, createdAt = now, updatedAt = now)
                )
            } else {
                noteDao.byId(id)?.let {
                    noteDao.upsert(
                        it.copy(title = title.trim(), body = body, pinned = pinned, updatedAt = now)
                    )
                }
                id
            }
        }

    fun toggleNotePin(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        noteDao.upsert(note.copy(pinned = !note.pinned, updatedAt = System.currentTimeMillis()))
    }

    fun deleteNote(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        noteDao.deleteById(note.id)
    }

    fun setNoteArchived(note: Note, archived: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        noteDao.upsert(note.copy(archived = archived, updatedAt = System.currentTimeMillis()))
    }

    // ---------------- یادآوری‌ها ----------------

    fun saveReminder(r: Reminder) = viewModelScope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val toSave = r.copy(createdAt = if (r.id == 0L) now else r.createdAt, updatedAt = now)
        val id = reminderDao.upsert(toSave)
        val rid = if (toSave.id != 0L) toSave.id else id
        val next = NextFire.computeNext(toSave.copy(id = rid), now)
        if (toSave.enabled && next != null) AlarmScheduler.schedule(context, rid, next)
        else AlarmScheduler.cancel(context, rid)
    }

    fun toggleReminder(r: Reminder, on: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val updated = r.copy(enabled = on, updatedAt = System.currentTimeMillis())
        reminderDao.upsert(updated)
        if (on) {
            val next = NextFire.computeNext(updated, System.currentTimeMillis())
            if (next != null) AlarmScheduler.schedule(context, r.id, next)
            else AlarmScheduler.cancel(context, r.id)
        } else {
            AlarmScheduler.cancel(context, r.id)
        }
    }

    fun deleteReminder(r: Reminder) = viewModelScope.launch(Dispatchers.IO) {
        AlarmScheduler.cancel(context, r.id)
        reminderDao.deleteById(r.id)
    }

    // ---------------- رویدادها ----------------

    fun saveEvent(e: Event) = viewModelScope.launch(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        eventDao.upsert(e.copy(createdAt = if (e.createdAt == 0L) now else e.createdAt, updatedAt = now))
    }

    fun deleteEvent(e: Event) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.deleteById(e.id)
    }

    // ---------------- جست‌وجوی سراسری ----------------

    suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext SearchResults()
        val hit: (String) -> Boolean = { PersianNormalizer.contains(it, q) }
        SearchResults(
            query = q,
            tasks = taskDao.all().filter { !it.deleted && hit(it.title + " " + (it.note ?: "")) },
            notes = noteDao.all().filter { !it.deleted && hit(it.title + " " + it.body) },
            events = eventDao.all().filter { !it.deleted && hit(it.title) },
            reminders = reminderDao.all().filter { !it.deleted && hit(it.title) }
        )
    }

    // ---------------- پشتیبان‌گیری ----------------

    suspend fun exportJson(): String = withContext(Dispatchers.IO) { BackupManager.export(db) }

    suspend fun importJson(json: String, replace: Boolean): Int =
        withContext(Dispatchers.IO) { BackupManager.import(db, context, json, replace) }
}
