package com.taskflowai.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.domain.model.AuthState
import com.taskflowai.domain.repository.PreferencesRepository
import com.taskflowai.domain.repository.UserRepository
import com.taskflowai.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _targetDestination = MutableStateFlow<String?>(null)
    val targetDestination: StateFlow<String?> = _targetDestination.asStateFlow()

    init {
        checkAppInitialization()
    }

    private fun checkAppInitialization() {
        viewModelScope.launch {
            // Elegant splash dwell time
            delay(1200)

            val isOnboarded = preferencesRepository.isOnboardingCompleted().first()
            if (!isOnboarded) {
                _targetDestination.value = Screen.Onboarding.route
                return@launch
            }

            val authState = userRepository.getAuthState().first()
            if (authState is AuthState.Authenticated) {
                _targetDestination.value = Screen.Dashboard.route
            } else {
                _targetDestination.value = Screen.Login.route
            }
        }
    }
}
