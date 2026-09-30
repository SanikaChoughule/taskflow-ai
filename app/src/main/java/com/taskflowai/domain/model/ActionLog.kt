package com.taskflowai.domain.model

enum class ActionType {
    COMMAND_RECEIVED,
    INTENT_IDENTIFIED,
    PARTICIPANT_RESOLVED,
    CALENDAR_CHECKED,
    CONFLICT_DETECTED,
    EVENT_CREATED,
    EVENT_UPDATED,
    EVENT_DELETED,
    REMINDER_CREATED,
    REMINDER_CANCELLED,
    TASK_COMPLETED,
    ACTION_UNDONE,
    ERROR_OCCURRED
}

enum class ActionStatus {
    INFO,
    SUCCESS,
    WARNING,
    FAILED
}

data class ActionLog(
    val id: String,
    val taskId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: ActionType,
    val status: ActionStatus = ActionStatus.INFO,
    val description: String,
    val metadata: Map<String, String> = emptyMap()
)
