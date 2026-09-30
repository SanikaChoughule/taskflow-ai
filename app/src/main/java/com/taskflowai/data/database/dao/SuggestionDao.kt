package com.taskflowai.data.database.dao

import androidx.room.*
import com.taskflowai.data.database.entity.SuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuggestionDao {
    @Query("SELECT * FROM suggestions WHERE isDismissed = 0 ORDER BY createdAt DESC")
    fun getActiveSuggestions(): Flow<List<SuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestions(suggestions: List<SuggestionEntity>)

    @Query("UPDATE suggestions SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissSuggestion(id: String)

    @Query("DELETE FROM suggestions WHERE isDismissed = 1")
    suspend fun deleteDismissed()
}
