package com.taskflowai.data.repository

import com.taskflowai.data.database.dao.ActionLogDao
import com.taskflowai.data.mapper.DataMappers.toDomain
import com.taskflowai.data.mapper.DataMappers.toEntity
import com.taskflowai.domain.model.ActionLog
import com.taskflowai.domain.repository.ActionLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ActionLogRepositoryImpl(
    private val actionLogDao: ActionLogDao
) : ActionLogRepository {

    override fun getActionLogsForTask(taskId: String): Flow<List<ActionLog>> {
        return actionLogDao.getActionLogsForTask(taskId).map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentActionLogs(limit: Int): Flow<List<ActionLog>> {
        return actionLogDao.getRecentActionLogs(limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertActionLog(log: ActionLog) {
        actionLogDao.insertActionLog(log.toEntity())
    }

    override suspend fun insertActionLogs(logs: List<ActionLog>) {
        actionLogDao.insertActionLogs(logs.map { it.toEntity() })
    }

    override suspend fun clearLogsForTask(taskId: String) {
        actionLogDao.deleteLogsForTask(taskId)
    }
}
