package com.taskflowai.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.taskflowai.presentation.components.VoicePulseIndicator
import com.taskflowai.presentation.theme.AccentCyan
import com.taskflowai.presentation.theme.PrimaryBlueLight

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    navController: NavController
) {
    val destination by viewModel.targetDestination.collectAsState()

    LaunchedEffect(destination) {
        destination?.let { dest ->
            navController.navigate(dest) {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.scale(scale)) {
                VoicePulseIndicator(isListening = true)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TaskFlow AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = PrimaryBlueLight
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "One sentence in. Every step handled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = AccentCyan,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
