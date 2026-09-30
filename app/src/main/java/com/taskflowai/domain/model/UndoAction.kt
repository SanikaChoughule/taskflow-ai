package com.taskflowai.domain.model

enum class UndoType {
    DELETE_CREATED_CALENDAR_EVENT,
    RESTORE_PREVIOUS_CALENDAR_EVENT,
    CANCEL_CREATED_REMINDER
}

data class UndoAction(
    val id: String,
    val taskId: String,
    val undoType: UndoType,
    val description: String,
    val targetId: String, // event ID or reminder ID
    val previousPayloadJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
