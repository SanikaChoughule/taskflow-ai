package com.taskflowai.domain.model

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class TaskStep(
    val id: String,
    val stepOrder: Int,
    val title: String,
    val description: String,
    val isDestructive: Boolean = false,
    val initialStatus: StepStatus = StepStatus.WAITING
)

data class TaskPlan(
    val planId: String,
    val intent: AIIntent,
    val summary: String,
    val entities: ParsedEntities,
    val steps: List<TaskStep>,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String? = null,
    val detectedConflict: Conflict? = null
)
