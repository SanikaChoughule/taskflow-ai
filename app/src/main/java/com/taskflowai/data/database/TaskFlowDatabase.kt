package com.taskflowai.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.taskflowai.data.database.dao.*
import com.taskflowai.data.database.entity.*

@Database(
    entities = [
        TaskEntity::class,
        SubTaskEntity::class,
        ActionLogEntity::class,
        CalendarEventEntity::class,
        ReminderEntity::class,
        SuggestionEntity::class,
        UserEntity::class,
        ScheduleHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TaskFlowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subTaskDao(): SubTaskDao
    abstract fun actionLogDao(): ActionLogDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun reminderDao(): ReminderDao
    abstract fun suggestionDao(): SuggestionDao
    abstract fun userDao(): UserDao
    abstract fun scheduleHistoryDao(): ScheduleHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TaskFlowDatabase? = null

        fun getInstance(context: Context): TaskFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TaskFlowDatabase::class.java,
                    "taskflow_ai.db"
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
