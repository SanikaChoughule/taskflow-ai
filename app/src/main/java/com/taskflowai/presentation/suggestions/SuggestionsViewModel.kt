package com.taskflowai.presentation.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.domain.model.Suggestion
import com.taskflowai.domain.repository.SuggestionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SuggestionsViewModel(
    private val suggestionRepository: SuggestionRepository
) : ViewModel() {

    val suggestions: StateFlow<List<Suggestion>> = suggestionRepository.getActiveSuggestions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun dismissSuggestion(id: String) {
        viewModelScope.launch {
            suggestionRepository.dismissSuggestion(id)
        }
    }

    fun refreshSuggestions() {
        viewModelScope.launch {
            suggestionRepository.refreshSuggestions()
        }
    }
}
