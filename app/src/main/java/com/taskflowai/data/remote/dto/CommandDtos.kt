package com.taskflowai.data.remote.dto

data class CommandRequestDto(
    val command: String,
    val userId: String,
    val timezone: String
)

data class TaskPlanResponseDto(
    val planId: String,
    val intent: String,
    val summary: String,
    val steps: List<TaskStepDto>,
    val riskLevel: String,
    val requiresConfirmation: Boolean,
    val confirmationPrompt: String?
)

data class TaskStepDto(
    val stepOrder: Int,
    val title: String,
    val description: String,
    val isDestructive: Boolean
)

data class CalendarSyncEventDto(
    val eventId: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val attendees: List<String>
)

data class UserSyncDto(
    val id: String,
    val email: String,
    val name: String,
    val photoUrl: String? = null,
    val isGoogleConnected: Boolean = false,
    val isCalendarConnected: Boolean = false,
    val lastLoginAt: Long = System.currentTimeMillis()
)

data class TaskSyncDto(
    val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val originalCommand: String = "",
    val status: String = "PENDING",
    val priority: String = "MEDIUM",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val linkedCalendarEventId: String? = null,
    val linkedReminderId: String? = null,
    val isUndoable: Boolean = true,
    val errorMessage: String? = null
)

data class HistorySyncDto(
    val id: String,
    val userId: String,
    val title: String,
    val scheduledDate: String,
    val startTimeFormatted: String,
    val endTimeFormatted: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val attendee: String? = null,
    val attendeeEmail: String? = null,
    val action: String = "SCHEDULED",
    val actionTime: Long = System.currentTimeMillis(),
    val calendarEventId: String
)

data class UndoSyncDto(
    val calendarEventId: String,
    val userId: String? = null
)

data class ApiResponseDto(
    val success: Boolean,
    val message: String? = null
)

