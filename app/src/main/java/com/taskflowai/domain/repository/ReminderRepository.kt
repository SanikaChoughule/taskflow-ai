package com.taskflowai.domain.repository

import com.taskflowai.domain.model.Reminder
import com.taskflowai.domain.model.ReminderStatus
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getUpcomingReminders(): Flow<List<Reminder>>
    suspend fun getReminderById(id: String): Reminder?
    suspend fun insertReminder(reminder: Reminder)
    suspend fun updateReminderStatus(id: String, status: ReminderStatus)
    suspend fun deleteReminder(id: String)
}
