package com.taskflowai.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.auth.GoogleAuthManager
import com.taskflowai.domain.model.AuthState
import com.taskflowai.domain.model.User
import com.taskflowai.domain.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val googleAuthManager: GoogleAuthManager
) : ViewModel() {

    val currentUser: StateFlow<User?> = userRepository.getAuthState()
        .map { (it as? AuthState.Authenticated)?.user }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            googleAuthManager.signOut()
            onSignedOut()
        }
    }
}
