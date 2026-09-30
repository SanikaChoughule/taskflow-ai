package com.taskflowai.data.database.dao

import androidx.room.*
import com.taskflowai.data.database.entity.ScheduleHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleHistoryDao {

    @Query("SELECT * FROM schedule_history WHERE :userId = '' OR userId = :userId OR userId = '' ORDER BY actionTime DESC")
    fun getHistoryForUser(userId: String): Flow<List<ScheduleHistoryEntity>>

    @Query("SELECT * FROM schedule_history ORDER BY actionTime DESC")
    fun getAllHistory(): Flow<List<ScheduleHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: ScheduleHistoryEntity)

    @Query("UPDATE schedule_history SET action = :action, actionTime = :actionTime WHERE calendarEventId = :calendarEventId")
    suspend fun updateActionByCalendarEventId(calendarEventId: String, action: String, actionTime: Long)

    @Query("DELETE FROM schedule_history WHERE calendarEventId = :calendarEventId")
    suspend fun deleteByCalendarEventId(calendarEventId: String)

    @Query("DELETE FROM schedule_history WHERE userId = :userId")
    suspend fun clearUserHistory(userId: String)

    @Query("DELETE FROM schedule_history")
    suspend fun clearAll()

    @Query("UPDATE schedule_history SET userId = :userId WHERE userId = '' OR userId IS NULL")
    suspend fun associateUnassignedHistoryToUser(userId: String)
}
