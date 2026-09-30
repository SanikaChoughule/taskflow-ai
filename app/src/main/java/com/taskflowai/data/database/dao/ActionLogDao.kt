package com.taskflowai.data.database.dao

import androidx.room.*
import com.taskflowai.data.database.entity.ActionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionLogDao {
    @Query("SELECT * FROM action_logs WHERE taskId = :taskId ORDER BY timestamp ASC")
    fun getActionLogsForTask(taskId: String): Flow<List<ActionLogEntity>>

    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentActionLogs(limit: Int): Flow<List<ActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLog(log: ActionLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLogs(logs: List<ActionLogEntity>)

    @Query("DELETE FROM action_logs WHERE taskId = :taskId")
    suspend fun deleteLogsForTask(taskId: String)
}
