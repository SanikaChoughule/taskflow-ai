package com.taskflowai.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.taskflowai.domain.model.ScheduleHistoryItem
import java.util.UUID

@Entity(tableName = "schedule_history")
data class ScheduleHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String,
    val scheduledDate: String,
    val startTimeFormatted: String,
    val endTimeFormatted: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val attendee: String? = null,
    val attendeeEmail: String? = null,
    val action: String, // "SCHEDULED" or "UNDONE"
    val actionTime: Long = System.currentTimeMillis(),
    val calendarEventId: String
) {
    fun toDomain(): ScheduleHistoryItem {
        return ScheduleHistoryItem(
            id = id,
            userId = userId,
            title = title,
            scheduledDate = scheduledDate,
            startTimeFormatted = startTimeFormatted,
            endTimeFormatted = endTimeFormatted,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            attendee = attendee,
            attendeeEmail = attendeeEmail,
            action = action,
            actionTime = actionTime,
            calendarEventId = calendarEventId
        )
    }

    companion object {
        fun fromDomain(item: ScheduleHistoryItem): ScheduleHistoryEntity {
            return ScheduleHistoryEntity(
                id = item.id,
                userId = item.userId,
                title = item.title,
                scheduledDate = item.scheduledDate,
                startTimeFormatted = item.startTimeFormatted,
                endTimeFormatted = item.endTimeFormatted,
                startTimeMs = item.startTimeMs,
                endTimeMs = item.endTimeMs,
                attendee = item.attendee,
                attendeeEmail = item.attendeeEmail,
                action = item.action,
                actionTime = item.actionTime,
                calendarEventId = item.calendarEventId
            )
        }
    }
}
