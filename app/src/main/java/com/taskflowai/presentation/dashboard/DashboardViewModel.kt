package com.taskflowai.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.domain.model.Suggestion
import com.taskflowai.domain.repository.SuggestionRepository
import com.taskflowai.domain.usecase.DashboardData
import com.taskflowai.domain.usecase.GetDashboardDataUseCase
import com.taskflowai.domain.usecase.UndoActionUseCase
import com.taskflowai.domain.usecase.UndoResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    getDashboardDataUseCase: GetDashboardDataUseCase,
    private val undoActionUseCase: UndoActionUseCase,
    private val suggestionRepository: SuggestionRepository
) : ViewModel() {

    val dashboardData: StateFlow<DashboardData?> = getDashboardDataUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun triggerUndo() {
        viewModelScope.launch {
            when (val result = undoActionUseCase()) {
                is UndoResult.Success -> _userMessage.emit(result.message)
                is UndoResult.Failure -> _userMessage.emit(result.error)
                UndoResult.NothingToUndo -> _userMessage.emit("No reversible action available.")
            }
        }
    }

    fun dismissSuggestion(suggestion: Suggestion) {
        viewModelScope.launch {
            suggestionRepository.dismissSuggestion(suggestion.id)
        }
    }
}
