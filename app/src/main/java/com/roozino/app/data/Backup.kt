package com.roozino.app.data

import android.content.Context
import android.net.Uri
import com.roozino.app.domain.PersianText
import org.json.JSONArray
import org.json.JSONObject

/**
 * پشتیبان‌گیری/بازیابی به‌صورت JSON از طریق Storage Access Framework.
 * کاربر خودش محل ذخیره را انتخاب می‌کند؛ هیچ داده‌ای جایی ارسال نمی‌شود.
 *
 * قالب فایل:
 * { "app": "roozino", "version": 1, "exportedAt": ..., "tasks": [...], "notes": [...] }
 */
object BackupManager {

    suspend fun buildJson(): String {
        val tasks = DataRepository.allTasks()
        val notes = DataRepository.allNotesList()
        val root = JSONObject()
        root.put("app", "roozino")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val tasksArr = JSONArray()
        tasks.forEach { t ->
            val o = JSONObject()
            o.put("title", t.title)
            o.put("dueDate", t.dueDate ?: JSONObject.NULL)
            o.put("dueTime", t.dueTime ?: JSONObject.NULL)
            o.put("reminderAt", t.reminderAt ?: JSONObject.NULL)
            o.put("isDone", t.isDone)
            o.put("createdAt", t.createdAt)
            o.put("updatedAt", t.updatedAt)
            tasksArr.put(o)
        }
        root.put("tasks", tasksArr)

        val notesArr = JSONArray()
        notes.forEach { n ->
            val o = JSONObject()
            o.put("title", n.title)
            o.put("content", n.content)
            o.put("createdAt", n.createdAt)
            o.put("updatedAt", n.updatedAt)
            notesArr.put(o)
        }
        root.put("notes", notesArr)
        return root.toString(2)
    }

    fun write(context: Context, uri: Uri, json: String): Boolean = try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
            out.flush()
            true
        } ?: false
    } catch (_: Exception) {
        false
    }

    fun read(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    } catch (_: Exception) {
        null
    }

    /** اعتبارسنجی و تبدیل JSON پشتیبان؛ ناموفق → null */
    fun parse(json: String): Pair<List<TaskEntity>, List<NoteEntity>>? {
        return try {
        val root = JSONObject(json)
        if (root.optString("app") != "roozino") return null
        val version = root.optInt("version", -1)
        if (version != 1) return null

        val tasks = mutableListOf<TaskEntity>()
        val tasksArr = root.optJSONArray("tasks") ?: return null
        for (i in 0 until tasksArr.length()) {
            val o = tasksArr.getJSONObject(i)
            val title = o.optString("title", "").trim()
            if (title.isEmpty()) continue
            val dueDate = if (o.isNull("dueDate")) null else o.optString("dueDate").takeIf { it.isNotBlank() }
            val dueTime = if (o.isNull("dueTime")) null else o.optString("dueTime").takeIf { it.isNotBlank() }
            val reminderAt = if (o.isNull("reminderAt")) null else o.optLong("reminderAt", 0L).takeIf { it > 0 }
            tasks.add(
                TaskEntity(
                    title = title,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    reminderAt = reminderAt,
                    isDone = o.optBoolean("isDone", false),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    searchNorm = PersianText.normalize(title)
                )
            )
        }

        val notes = mutableListOf<NoteEntity>()
        val notesArr = root.optJSONArray("notes") ?: return null
        for (i in 0 until notesArr.length()) {
            val o = notesArr.getJSONObject(i)
            val content = o.optString("content", "")
            val title = o.optString("title", "")
            if (content.isBlank() && title.isBlank()) continue
            notes.add(
                NoteEntity(
                    title = title,
                    content = content,
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    searchNorm = PersianText.normalize("$title $content")
                )
            )
        }
        tasks to notes
        } catch (_: Exception) {
            null
        }
    }
}
