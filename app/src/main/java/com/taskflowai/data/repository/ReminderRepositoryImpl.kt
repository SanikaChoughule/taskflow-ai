package com.taskflowai.data.repository

import com.taskflowai.data.database.dao.ReminderDao
import com.taskflowai.data.mapper.DataMappers.toDomain
import com.taskflowai.data.mapper.DataMappers.toEntity
import com.taskflowai.domain.model.Reminder
import com.taskflowai.domain.model.ReminderStatus
import com.taskflowai.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders().map { list -> list.map { it.toDomain() } }
    }

    override fun getUpcomingReminders(): Flow<List<Reminder>> {
        return reminderDao.getUpcomingReminders().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getReminderById(id: String): Reminder? {
        return reminderDao.getReminderById(id)?.toDomain()
    }

    override suspend fun insertReminder(reminder: Reminder) {
        reminderDao.insertReminder(reminder.toEntity())
    }

    override suspend fun updateReminderStatus(id: String, status: ReminderStatus) {
        reminderDao.updateReminderStatus(id, status.name)
    }

    override suspend fun deleteReminder(id: String) {
        reminderDao.deleteReminderById(id)
    }
}
