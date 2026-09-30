package com.taskflowai.domain.model

enum class AIIntent {
    SCHEDULE_EVENT,
    RESCHEDULE_EVENT,
    CANCEL_EVENT,
    QUERY_SCHEDULE,
    FIND_FREE_SLOT,
    CREATE_REMINDER,
    UNKNOWN
}

data class ParsedEntities(
    val title: String = "",
    val participant: String? = null,
    val dateString: String? = null,
    val timeString: String? = null,
    val targetTimestamp: Long? = null,
    val durationMinutes: Int = 60,
    val reminderMinutesBefore: Int? = null,
    val rawTargetMeeting: String? = null
)

data class AICommand(
    val id: String,
    val rawText: String,
    val intent: AIIntent,
    val entities: ParsedEntities,
    val confidence: Float = 1.0f,
    val explanation: String = ""
)
