package com.taskflowai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.auth.GoogleAuthManager
import com.taskflowai.calendar.CalendarService
import com.taskflowai.domain.model.AuthState
import com.taskflowai.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val calendarService: CalendarService,
    private val googleAuthManager: GoogleAuthManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _connectedEmail = MutableStateFlow<String?>("user@gmail.com")
    val connectedEmail: StateFlow<String?> = _connectedEmail.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    val appVersion: String = "1.0.0"

    init {
        viewModelScope.launch {
            userRepository.getAuthState().collect { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        _connectedEmail.value = authState.user.email
                        _isConnected.value = authState.user.isGoogleConnected
                    }
                    else -> {
                        val current = userRepository.getCurrentUser()
                        _connectedEmail.value = current?.email ?: googleAuthManager.getConnectedEmail()
                        _isConnected.value = current?.isGoogleConnected ?: false
                    }
                }
            }
        }
    }

    /**
     * Signs out of Google session, clears stored tokens and session data,
     * without deleting any existing Google Calendar events.
     */
    fun disconnectGoogleAccount(onDisconnected: () -> Unit) {
        viewModelScope.launch {
            calendarService.disconnect()
            googleAuthManager.signOut()
            _isConnected.value = false
            _connectedEmail.value = null
            onDisconnected()
        }
    }
}
