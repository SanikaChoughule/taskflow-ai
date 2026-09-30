package com.taskflowai.data.database.dao

import androidx.room.*
import com.taskflowai.data.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET isCalendarConnected = :connected WHERE id = :userId")
    suspend fun updateCalendarConnected(userId: String, connected: Boolean)

    @Query("DELETE FROM users")
    suspend fun clearUser()
}
