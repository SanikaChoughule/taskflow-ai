package com.taskflowai.domain.model

data class ScheduleHistoryItem(
    val id: String,
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
    val actionTime: Long,
    val calendarEventId: String
)
