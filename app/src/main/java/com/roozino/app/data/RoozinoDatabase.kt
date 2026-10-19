package com.roozino.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TaskEntity::class, NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RoozinoDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: RoozinoDatabase? = null

        fun get(context: Context): RoozinoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoozinoDatabase::class.java,
                    "roozino.db"
                ).build().also { INSTANCE = it }
            }
    }
}
