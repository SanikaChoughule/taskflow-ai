package com.taskflowai.domain.model

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String = "",
    val location: String = "",
    val startTime: Long,
    val endTime: Long,
    val attendees: List<String> = emptyList(),
    val calendarName: String = "Primary Calendar",
    val colorTag: String = "#2563EB",
    val isAllDay: Boolean = false,
    val googleEventId: String? = null
)

enum class ConflictType {
    EXACT_OVERLAP,
    PARTIAL_OVERLAP,
    BACK_TO_BACK
}

data class Conflict(
    val conflictingEvent: CalendarEvent,
    val conflictType: ConflictType,
    val message: String
)
