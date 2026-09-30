package com.taskflowai.data.repository

import com.taskflowai.ai.AIService
import com.taskflowai.data.database.dao.SuggestionDao
import com.taskflowai.data.mapper.DataMappers.toDomain
import com.taskflowai.data.mapper.DataMappers.toEntity
import com.taskflowai.domain.model.Suggestion
import com.taskflowai.domain.repository.SuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SuggestionRepositoryImpl(
    private val suggestionDao: SuggestionDao,
    private val aiService: AIService
) : SuggestionRepository {

    override fun getActiveSuggestions(): Flow<List<Suggestion>> {
        return suggestionDao.getActiveSuggestions().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertSuggestions(suggestions: List<Suggestion>) {
        suggestionDao.insertSuggestions(suggestions.map { it.toEntity() })
    }

    override suspend fun dismissSuggestion(id: String) {
        suggestionDao.dismissSuggestion(id)
    }

    override suspend fun refreshSuggestions() {
        val newSuggestions = aiService.generateSuggestions()
        suggestionDao.insertSuggestions(newSuggestions.map { it.toEntity() })
    }
}
