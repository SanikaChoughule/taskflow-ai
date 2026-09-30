package com.taskflowai.domain.repository

import com.taskflowai.domain.model.Suggestion
import kotlinx.coroutines.flow.Flow

interface SuggestionRepository {
    fun getActiveSuggestions(): Flow<List<Suggestion>>
    suspend fun insertSuggestions(suggestions: List<Suggestion>)
    suspend fun dismissSuggestion(id: String)
    suspend fun refreshSuggestions()
}
