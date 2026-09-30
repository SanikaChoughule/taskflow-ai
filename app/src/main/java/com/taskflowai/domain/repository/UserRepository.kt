package com.taskflowai.domain.repository

import com.taskflowai.domain.model.AuthState
import com.taskflowai.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getAuthState(): Flow<AuthState>
    suspend fun getCurrentUser(): User?
    suspend fun saveUser(user: User)
    suspend fun updateCalendarConnected(isConnected: Boolean)
    suspend fun signOut()
}
