package com.taskflowai.domain.repository

import com.taskflowai.domain.model.ScheduleHistoryItem
import kotlinx.coroutines.flow.Flow

interface ScheduleHistoryRepository {
    fun getAllHistory(): Flow<List<ScheduleHistoryItem>>
    fun getHistoryForUser(userId: String): Flow<List<ScheduleHistoryItem>>
    suspend fun recordScheduled(item: ScheduleHistoryItem)
    suspend fun recordUndone(calendarEventId: String)
    suspend fun clearHistory(userId: String? = null)
}
