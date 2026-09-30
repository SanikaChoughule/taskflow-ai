package com.taskflowai.domain.model

data class TaskExecutionResult(
    val taskId: String,
    val isSuccess: Boolean,
    val message: String,
    val createdEvent: CalendarEvent? = null,
    val createdReminder: Reminder? = null,
    val executedStepsCount: Int = 0,
    val actionLogs: List<ActionLog> = emptyList(),
    val undoAction: UndoAction? = null
)
