package com.taskflowai.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val taskId: String?,
    val calendarEventId: String?,
    val title: String,
    val description: String,
    val triggerTime: Long,
    val timingMinutesBefore: Int,
    val status: String,
    val createdAt: Long
)
