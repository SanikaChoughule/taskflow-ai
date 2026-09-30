package com.taskflowai.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taskflowai.domain.repository.PreferencesRepository
import com.taskflowai.domain.repository.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "taskflow_preferences")

class PreferencesRepositoryImpl(
    private val context: Context
) : PreferencesRepository {

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val VOICE_CONFIRMATION = booleanPreferencesKey("voice_confirmation")
    }

    override fun isOnboardingCompleted(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    override fun getThemeMode(): Flow<ThemeMode> {
        return context.dataStore.data.map { preferences ->
            val modeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            runCatching { ThemeMode.valueOf(modeStr) }.getOrDefault(ThemeMode.SYSTEM)
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override fun getAiProvider(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.AI_PROVIDER] ?: "LOCAL"
        }
    }

    override suspend fun setAiProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_PROVIDER] = provider
        }
    }

    override fun isBiometricEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    override fun isVoiceConfirmationRequired(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.VOICE_CONFIRMATION] ?: true
        }
    }

    override suspend fun setVoiceConfirmationRequired(required: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_CONFIRMATION] = required
        }
    }
}
