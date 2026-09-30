package com.taskflowai.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.taskflowai.app.AppContainer
import com.taskflowai.domain.model.AuthState
import com.taskflowai.presentation.auth.AuthViewModel
import com.taskflowai.presentation.auth.LoginScreen
import com.taskflowai.presentation.history.HistoryScreen
import com.taskflowai.presentation.history.HistoryViewModel
import com.taskflowai.presentation.home.HomeScreen
import com.taskflowai.presentation.home.HomeViewModel
import com.taskflowai.presentation.settings.SettingsScreen
import com.taskflowai.presentation.settings.SettingsViewModel

@Composable
fun TaskFlowNavGraph(
    navController: NavHostController,
    container: AppContainer
) {
    val authState by container.userRepository.getAuthState().collectAsState(initial = null)

    // While checking initial authentication session, show clean background
    if (authState == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    val startDestination = if (authState is AuthState.Authenticated) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }

    val imeInsets = androidx.compose.foundation.layout.WindowInsets.ime
    val density = androidx.compose.ui.platform.LocalDensity.current
    val isImeVisible = imeInsets.getBottom(density) > 0

    Scaffold(
        bottomBar = {
            if (!isImeVisible) {
                TaskFlowBottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 0. GOOGLE SIGN-IN SCREEN (First step for unauthenticated users)
            composable(Screen.Login.route) {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                    AuthViewModel(
                        googleAuthManager = container.googleAuthManager,
                        userRepository = container.userRepository
                    )
                }
                LoginScreen(viewModel = viewModel, navController = navController)
            }

            // 1. HOME SCREEN (Simple AI Voice Assistant & Upcoming Plans)
            composable(Screen.Home.route) {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                    HomeViewModel(
                        calendarService = container.calendarService,
                        scheduleHistoryRepository = container.scheduleHistoryRepository,
                        voiceRecognizerManager = container.voiceRecognizerManager,
                        userRepository = container.userRepository,
                        calendarRepository = container.calendarRepository
                    )
                }
                HomeScreen(viewModel = viewModel, navController = navController)
            }

            // 2. HISTORY SCREEN (Chronological activity log with SCHEDULED / UNDONE)
            composable(Screen.History.route) {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                    HistoryViewModel(
                        scheduleHistoryRepository = container.scheduleHistoryRepository,
                        userRepository = container.userRepository
                    )
                }
                HistoryScreen(viewModel = viewModel, navController = navController)
            }

            // 3. SETTINGS SCREEN (Google Calendar connection status & Disconnect)
            composable(Screen.Settings.route) {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                    SettingsViewModel(
                        calendarService = container.calendarService,
                        googleAuthManager = container.googleAuthManager,
                        userRepository = container.userRepository
                    )
                }
                SettingsScreen(viewModel = viewModel, navController = navController)
            }
        }
    }
}
