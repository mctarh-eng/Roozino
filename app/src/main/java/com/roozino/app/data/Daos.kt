package com.roozino.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query(
        "SELECT * FROM tasks WHERE isDone = 0 ORDER BY dueDate IS NULL, dueDate, " +
            "dueTime IS NULL, dueTime, createdAt DESC"
    )
    fun activeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDone = 1 ORDER BY updatedAt DESC")
    fun doneTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date AND isDone = 0 ORDER BY dueTime IS NULL, dueTime")
    fun tasksForDate(date: String): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE dueDate >= :fromIso AND dueDate <= :toIso " +
            "ORDER BY dueDate, dueTime IS NULL, dueTime"
    )
    suspend fun tasksInRange(fromIso: String, toIso: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE reminderAt IS NOT NULL AND reminderAt > :now AND isDone = 0")
    suspend fun futureReminders(now: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE searchNorm LIKE '%' || :q || '%' ESCAPE '\\' ORDER BY updatedAt DESC")
    suspend fun search(q: String): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    suspend fun all(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun byId(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>): List<Long>

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET isDone = :done, updatedAt = :time WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, time: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun allNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE createdAt >= :from AND createdAt < :to ORDER BY createdAt DESC")
    suspend fun createdBetween(from: Long, to: Long): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE searchNorm LIKE '%' || :q || '%' ESCAPE '\\' ORDER BY updatedAt DESC")
    suspend fun search(q: String): List<NoteEntity>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun all(): List<NoteEntity>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Insert
    suspend fun insertAll(notes: List<NoteEntity>): List<Long>

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}
