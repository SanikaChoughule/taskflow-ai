package com.taskflowai.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null,
    val isGoogleConnected: Boolean = false,
    val isCalendarConnected: Boolean = false,
    val lastLoginAt: Long = System.currentTimeMillis()
)

sealed interface AuthState {
    data object Loading : AuthState
    data class Authenticated(val user: User) : AuthState
    data object Unauthenticated : AuthState
    data class Error(val message: String) : AuthState
}
