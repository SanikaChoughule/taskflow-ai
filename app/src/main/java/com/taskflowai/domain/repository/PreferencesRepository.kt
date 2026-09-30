package com.taskflowai.domain.repository

import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

interface PreferencesRepository {
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)

    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    fun getAiProvider(): Flow<String>
    suspend fun setAiProvider(provider: String)

    fun isBiometricEnabled(): Flow<Boolean>
    suspend fun setBiometricEnabled(enabled: Boolean)

    fun isVoiceConfirmationRequired(): Flow<Boolean>
    suspend fun setVoiceConfirmationRequired(required: Boolean)
}
