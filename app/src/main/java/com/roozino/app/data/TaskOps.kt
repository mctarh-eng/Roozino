package com.roozino.app.data

import android.content.Context
import com.roozino.app.notif.AlarmScheduler
import com.roozino.app.notif.NextFire
import com.roozino.app.util.TaskRepeat

/** منطق مشترک تکمیل کار (با پشتیبانی تکرار) — هم در UI و هم در اعلان استفاده می‌شود */
object TaskOps {

    suspend fun completeTask(db: AppDatabase, context: Context?, task: Task) {
        val now = System.currentTimeMillis()
        val taskDao = db.taskDao()
        val reminderDao = db.reminderDao()

        if (task.repeatType != 0 && task.dueMillis != null) {
            // کار تکرارشونده: به‌جای بستن، رخداد بعدی ساخته می‌شود
            val nextDue = TaskRepeat.next(task.dueMillis!!, task.repeatType)
            taskDao.upsert(
                task.copy(isDone = false, doneAt = null, dueMillis = nextDue, updatedAt = now)
            )
            if (task.reminderEnabled && nextDue != null) {
                val rem = reminderDao.all().firstOrNull { it.taskRef == task.id && !it.deleted }
                if (rem != null) {
                    val updated = rem.copy(timeMillis = nextDue, enabled = true, updatedAt = now)
                    val rid = reminderDao.upsert(updated)
                    context?.let {
                        AlarmScheduler.schedule(it, if (rem.id != 0L) rem.id else rid, nextDue)
                    }
                }
            }
        } else {
            taskDao.upsert(task.copy(isDone = true, doneAt = now, updatedAt = now))
            val rem = reminderDao.all().firstOrNull { it.taskRef == task.id && !it.deleted }
            if (rem != null && rem.enabled) {
                context?.let { AlarmScheduler.cancel(it, rem.id) }
                reminderDao.upsert(rem.copy(enabled = false, updatedAt = now))
            }
        }
    }
}
