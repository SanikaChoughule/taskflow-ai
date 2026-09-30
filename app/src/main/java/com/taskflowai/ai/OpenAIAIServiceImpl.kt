package com.taskflowai.ai

import com.taskflowai.BuildConfig
import com.taskflowai.domain.model.*

class OpenAIAIServiceImpl(
    private val localFallback: LocalRuleBasedAIServiceImpl = LocalRuleBasedAIServiceImpl()
) : AIService {

    override suspend fun analyzeCommand(rawText: String): Result<AICommand> {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isBlank() || apiKey == "\"\"") {
            return localFallback.analyzeCommand(rawText)
        }
        return try {
            localFallback.analyzeCommand(rawText)
        } catch (e: Exception) {
            localFallback.analyzeCommand(rawText)
        }
    }

    override suspend fun createTaskPlan(command: AICommand, existingConflict: Conflict?): Result<TaskPlan> {
        return localFallback.createTaskPlan(command, existingConflict)
    }

    override suspend fun generateSuggestions(): List<Suggestion> {
        return localFallback.generateSuggestions()
    }
}
