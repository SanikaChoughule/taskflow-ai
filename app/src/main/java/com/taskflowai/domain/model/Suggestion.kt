package com.taskflowai.domain.model

enum class SuggestionType {
    PRE_MEETING_REMINDER,
    FREE_SLOT_AVAILABLE,
    CONFLICT_RESOLVED,
    FOLLOW_UP,
    TASK_OPTIMIZATION
}

data class Suggestion(
    val id: String,
    val title: String,
    val message: String,
    val actionCommand: String,
    val type: SuggestionType,
    val isDismissed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
