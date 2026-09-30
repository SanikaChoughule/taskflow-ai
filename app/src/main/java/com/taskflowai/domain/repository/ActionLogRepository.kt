package com.taskflowai.domain.repository

import com.taskflowai.domain.model.ActionLog
import kotlinx.coroutines.flow.Flow

interface ActionLogRepository {
    fun getActionLogsForTask(taskId: String): Flow<List<ActionLog>>
    fun getRecentActionLogs(limit: Int = 10): Flow<List<ActionLog>>
    suspend fun insertActionLog(log: ActionLog)
    suspend fun insertActionLogs(logs: List<ActionLog>)
    suspend fun clearLogsForTask(taskId: String)
}
