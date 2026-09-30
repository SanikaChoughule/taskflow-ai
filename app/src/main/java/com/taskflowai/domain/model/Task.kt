package com.taskflowai.domain.model

enum class TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

data class Task(
    val id: String,
    val userId: String = "",
    val title: String,
    val description: String = "",
    val originalCommand: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val subTasks: List<SubTask> = emptyList(),
    val linkedCalendarEventId: String? = null,
    val linkedReminderId: String? = null,
    val isUndoable: Boolean = false,
    val errorMessage: String? = null
)
