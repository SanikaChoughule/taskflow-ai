package com.taskflowai.security

import android.content.Context
import android.content.SharedPreferences

class SecureTokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("taskflow_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
    }

    fun saveTokens(authToken: String, refreshToken: String? = null, userId: String? = null) {
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, authToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, userId)
            apply()
        }
    }

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)
    fun getAccessToken(): String? = getAuthToken()
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}
