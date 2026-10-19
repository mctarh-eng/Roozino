package com.roozino.app.notif

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.roozino.app.data.AppDatabase
import com.roozino.app.data.TaskOps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** دکمه‌های اعلان: «انجام شد» و «۱۰ دقیقه دیگر» */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra(ACTION) ?: return
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1L)
        if (id <= 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(id.toInt())
                val r = db.reminderDao().byId(id) ?: return@launch
                when (action) {
                    ACTION_COMPLETE -> {
                        val task = r.taskRef?.let { tid -> db.taskDao().byId(tid) }
                        if (task != null) {
                            TaskOps.completeTask(db, context, task)
                        } else {
                            val next = NextFire.computeNext(r, System.currentTimeMillis())
                            if (next != null) {
                                AlarmScheduler.schedule(context, r.id, next)
                            } else {
                                AlarmScheduler.cancel(context, r.id)
                                db.reminderDao().upsert(
                                    r.copy(enabled = false, updatedAt = System.currentTimeMillis())
                                )
                            }
                        }
                    }
                    ACTION_SNOOZE -> {
                        AlarmScheduler.schedule(
                            context, r.id, System.currentTimeMillis() + 10 * 60_000L
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "action"
        const val ACTION_COMPLETE = "complete"
        const val ACTION_SNOOZE = "snooze"
    }
}
