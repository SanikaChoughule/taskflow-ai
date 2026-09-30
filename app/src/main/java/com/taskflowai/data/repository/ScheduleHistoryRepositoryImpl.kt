package com.taskflowai.data.repository

import com.taskflowai.data.database.dao.ScheduleHistoryDao
import com.taskflowai.data.database.entity.ScheduleHistoryEntity
import com.taskflowai.data.remote.api.TaskFlowApi
import com.taskflowai.data.remote.dto.HistorySyncDto
import com.taskflowai.data.remote.dto.UndoSyncDto
import com.taskflowai.domain.model.ScheduleHistoryItem
import com.taskflowai.domain.repository.ScheduleHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ScheduleHistoryRepositoryImpl(
    private val dao: ScheduleHistoryDao,
    private val api: TaskFlowApi? = null
) : ScheduleHistoryRepository {

    override fun getAllHistory(): Flow<List<ScheduleHistoryItem>> {
        return dao.getAllHistory().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getHistoryForUser(userId: String): Flow<List<ScheduleHistoryItem>> {
        return dao.getHistoryForUser(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun recordScheduled(item: ScheduleHistoryItem) {
        dao.insertHistory(ScheduleHistoryEntity.fromDomain(item))

        if (api != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    api.saveUserHistory(
                        HistorySyncDto(
                            id = item.id,
                            userId = item.userId,
                            title = item.title,
                            scheduledDate = item.scheduledDate,
                            startTimeFormatted = item.startTimeFormatted,
                            endTimeFormatted = item.endTimeFormatted,
                            startTimeMs = item.startTimeMs,
                            endTimeMs = item.endTimeMs,
                            attendee = item.attendee,
                            attendeeEmail = item.attendeeEmail,
                            action = item.action,
                            actionTime = item.actionTime,
                            calendarEventId = item.calendarEventId
                        )
                    )
                }
            }
        }
    }

    override suspend fun recordUndone(calendarEventId: String) {
        dao.updateActionByCalendarEventId(
            calendarEventId = calendarEventId,
            action = "UNDONE",
            actionTime = System.currentTimeMillis()
        )

        if (api != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    api.undoUserHistory(UndoSyncDto(calendarEventId = calendarEventId))
                }
            }
        }
    }

    override suspend fun clearHistory(userId: String?) {
        if (!userId.isNullOrBlank()) {
            dao.clearUserHistory(userId)
            if (api != null) {
                withContext(Dispatchers.IO) {
                    runCatching { api.clearUserHistory(userId) }
                }
            }
        } else {
            dao.clearAll()
        }
    }
}
