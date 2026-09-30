package com.taskflowai.ai

import com.taskflowai.domain.model.AICommand
import com.taskflowai.domain.model.Conflict
import com.taskflowai.domain.model.Suggestion
import com.taskflowai.domain.model.TaskPlan

interface AIService {
    suspend fun analyzeCommand(rawText: String): Result<AICommand>
    suspend fun createTaskPlan(command: AICommand, existingConflict: Conflict?): Result<TaskPlan>
    suspend fun generateSuggestions(): List<Suggestion>
}
