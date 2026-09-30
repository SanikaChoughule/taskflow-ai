package com.taskflowai.domain.model

enum class StepStatus {
    WAITING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED
}

data class SubTask(
    val id: String,
    val taskId: String,
    val stepOrder: Int,
    val title: String,
    val description: String = "",
    val status: StepStatus = StepStatus.WAITING,
    val executedAt: Long? = null,
    val errorMessage: String? = null
)
