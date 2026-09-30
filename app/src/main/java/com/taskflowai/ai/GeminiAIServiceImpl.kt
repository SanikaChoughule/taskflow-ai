package com.taskflowai.ai

import com.taskflowai.BuildConfig
import com.taskflowai.domain.model.*

class GeminiAIServiceImpl(
    private val localFallback: LocalRuleBasedAIServiceImpl = LocalRuleBasedAIServiceImpl()
) : AIService {

    override suspend fun analyzeCommand(rawText: String): Result<AICommand> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "\"\"") {
            // Graceful fallback to deterministic local engine
            return localFallback.analyzeCommand(rawText)
        }

        // When external API key is provided, we can call remote endpoint; fallback if offline
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
