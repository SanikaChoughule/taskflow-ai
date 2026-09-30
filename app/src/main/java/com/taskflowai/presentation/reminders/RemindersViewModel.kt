package com.taskflowai.presentation.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.domain.model.Reminder
import com.taskflowai.domain.usecase.ManageRemindersUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersViewModel(
    private val manageRemindersUseCase: ManageRemindersUseCase
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = manageRemindersUseCase.getAllReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun dismissReminder(id: String) {
        viewModelScope.launch {
            manageRemindersUseCase.dismissReminder(id)
        }
    }

    fun cancelReminder(id: String) {
        viewModelScope.launch {
            manageRemindersUseCase.cancelReminder(id)
        }
    }
}
