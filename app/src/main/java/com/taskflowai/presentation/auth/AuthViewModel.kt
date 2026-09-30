package com.taskflowai.presentation.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.auth.GoogleAuthManager
import com.taskflowai.domain.model.AuthState
import com.taskflowai.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val googleAuthManager: GoogleAuthManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _emailInput = MutableStateFlow("")
    val emailInput: StateFlow<String> = _emailInput.asStateFlow()

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getAuthState().collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onEmailChanged(email: String) {
        _emailInput.value = email
        _errorMessage.value = null
    }

    fun onPasswordChanged(password: String) {
        _passwordInput.value = password
        _errorMessage.value = null
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    fun getGoogleSignInIntent(): Intent {
        _errorMessage.value = null
        return googleAuthManager.getSignInIntent()
    }

    fun onGoogleSignInStarted() {
        _uiState.value = AuthState.Loading
        _errorMessage.value = null
    }

    fun handleGoogleSignInResult(data: Intent?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            val result = googleAuthManager.handleSignInResult(data)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthState.Authenticated(user)
                    _errorMessage.value = null
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = AuthState.Unauthenticated
                    val message = error.message ?: "Unable to sign in with Google. Please try again."
                    _errorMessage.value = message
                }
            )
        }
    }

    fun signInWithGmailPassword(onSuccess: () -> Unit) {
        val email = _emailInput.value.trim()
        val password = _passwordInput.value

        if (email.isBlank()) {
            _errorMessage.value = "Please enter your Gmail address."
            return
        }
        if (!email.contains("@") || !email.contains(".")) {
            _errorMessage.value = "Please enter a valid Gmail address."
            return
        }
        if (password.isBlank()) {
            _errorMessage.value = "Please enter your password."
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            _errorMessage.value = null

            val result = googleAuthManager.signInWithGmail(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthState.Authenticated(user)
                    _errorMessage.value = null
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = AuthState.Unauthenticated
                    _errorMessage.value = error.message ?: "Failed to sign in with Gmail. Please check your credentials."
                }
            )
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
