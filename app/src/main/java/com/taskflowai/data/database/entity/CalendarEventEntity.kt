package com.taskflowai.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val location: String,
    val startTime: Long,
    val endTime: Long,
    val attendeesJson: String,
    val calendarName: String,
    val colorTag: String,
    val isAllDay: Boolean,
    val googleEventId: String?
)
