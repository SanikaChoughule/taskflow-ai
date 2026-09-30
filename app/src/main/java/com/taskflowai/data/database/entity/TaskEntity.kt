package com.taskflowai.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val title: String,
    val description: String,
    val originalCommand: String,
    val status: String,
    val priority: String,
    val createdAt: Long,
    val completedAt: Long?,
    val linkedCalendarEventId: String?,
    val linkedReminderId: String?,
    val isUndoable: Boolean,
    val errorMessage: String?
)
