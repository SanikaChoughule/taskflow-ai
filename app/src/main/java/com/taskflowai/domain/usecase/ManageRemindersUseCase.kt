package com.taskflowai.domain.usecase

import com.taskflowai.domain.model.Reminder
import com.taskflowai.domain.model.ReminderStatus
import com.taskflowai.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow

class ManageRemindersUseCase(
    private val reminderRepository: ReminderRepository
) {
    fun getUpcomingReminders(): Flow<List<Reminder>> = reminderRepository.getUpcomingReminders()
    fun getAllReminders(): Flow<List<Reminder>> = reminderRepository.getAllReminders()

    suspend fun createReminder(reminder: Reminder) {
        reminderRepository.insertReminder(reminder)
    }

    suspend fun dismissReminder(id: String) {
        reminderRepository.updateReminderStatus(id, ReminderStatus.DISMISSED)
    }

    suspend fun cancelReminder(id: String) {
        reminderRepository.updateReminderStatus(id, ReminderStatus.CANCELLED)
    }
}
