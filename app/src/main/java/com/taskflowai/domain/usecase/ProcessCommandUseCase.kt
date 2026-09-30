package com.taskflowai.domain.usecase

import com.taskflowai.ai.AIService
import com.taskflowai.domain.model.AICommand
import com.taskflowai.domain.model.AIIntent
import com.taskflowai.domain.model.Conflict
import com.taskflowai.domain.model.TaskPlan
import com.taskflowai.domain.repository.CalendarRepository

sealed class CommandValidationResult {
    data class Valid(val command: AICommand, val plan: TaskPlan, val conflict: Conflict?) : CommandValidationResult()
    data class Ambiguous(val message: String) : CommandValidationResult()
    data class Error(val error: String) : CommandValidationResult()
}

class ProcessCommandUseCase(
    private val aiService: AIService,
    private val calendarRepository: CalendarRepository
) {
    suspend operator fun invoke(rawInput: String): CommandValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return CommandValidationResult.Error("Command cannot be empty. Please type or speak a request.")
        }

        // 1. AI Intent & Entity Extraction
        val commandResult = aiService.analyzeCommand(trimmed)
        if (commandResult.isFailure) {
            return CommandValidationResult.Error(
                commandResult.exceptionOrNull()?.message ?: "Unable to understand command."
            )
        }
        val command = commandResult.getOrThrow()

        if (command.intent == AIIntent.UNKNOWN) {
            return CommandValidationResult.Ambiguous(
                "TaskFlow AI could not determine the exact intent. Please specify a schedule, reminder, or query request."
            )
        }

        // 2. Conflict Detection for scheduling intents
        var detectedConflict: Conflict? = null
        val targetTimestamp = command.entities.targetTimestamp
        if (targetTimestamp != null &&
            (command.intent == AIIntent.SCHEDULE_EVENT || command.intent == AIIntent.RESCHEDULE_EVENT)
        ) {
            val endTime = targetTimestamp + (command.entities.durationMinutes * 60 * 1000L)
            detectedConflict = calendarRepository.checkConflict(targetTimestamp, endTime)
        }

        // 3. Generate structured task plan
        val planResult = aiService.createTaskPlan(command, detectedConflict)
        if (planResult.isFailure) {
            return CommandValidationResult.Error("Failed to formulate task execution plan.")
        }
        val plan = planResult.getOrThrow()

        return CommandValidationResult.Valid(
            command = command,
            plan = plan,
            conflict = detectedConflict
        )
    }
}
