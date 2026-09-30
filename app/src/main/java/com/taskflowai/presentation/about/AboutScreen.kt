package com.taskflowai.presentation.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.taskflowai.presentation.components.VoicePulseIndicator
import com.taskflowai.presentation.theme.PrimaryBlueLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About TaskFlow AI") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            VoicePulseIndicator(isListening = false)

            Text(
                text = "TaskFlow AI",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = PrimaryBlueLight
            )

            Text(
                text = "\"One sentence in. Every step handled. Nothing forgotten.\"",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version: 1.0.0 (Production Stable)", style = MaterialTheme.typography.bodyMedium)
                    Text("Architecture: Clean Architecture & MVVM", style = MaterialTheme.typography.bodyMedium)
                    Text("UI Toolkit: Jetpack Compose & Material 3", style = MaterialTheme.typography.bodyMedium)
                    Text("Database: Room ORM (SQLite)", style = MaterialTheme.typography.bodyMedium)
                    Text("Background Scheduler: AndroidX WorkManager", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text(
                text = "TaskFlow AI is an autonomous multi-step task execution agent designed to turn natural speech into validated, explainable, and reversible daily actions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
