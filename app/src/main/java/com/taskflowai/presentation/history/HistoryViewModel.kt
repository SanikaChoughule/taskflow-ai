package com.taskflowai.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.domain.model.AuthState
import com.taskflowai.domain.model.ScheduleHistoryItem
import com.taskflowai.domain.repository.ScheduleHistoryRepository
import com.taskflowai.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val scheduleHistoryRepository: ScheduleHistoryRepository,
    private val userRepository: UserRepository? = null
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems: StateFlow<List<ScheduleHistoryItem>> = if (userRepository != null) {
        userRepository.getAuthState().flatMapLatest { authState ->
            val userId = (authState as? AuthState.Authenticated)?.user?.id ?: ""
            scheduleHistoryRepository.getHistoryForUser(userId)
        }
    } else {
        scheduleHistoryRepository.getAllHistory()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun clearAllHistory() {
        viewModelScope.launch {
            val userId = userRepository?.getCurrentUser()?.id
            scheduleHistoryRepository.clearHistory(userId)
        }
    }
}
