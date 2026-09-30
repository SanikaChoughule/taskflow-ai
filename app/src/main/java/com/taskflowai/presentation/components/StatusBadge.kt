package com.taskflowai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taskflowai.domain.model.StepStatus
import com.taskflowai.domain.model.TaskStatus
import com.taskflowai.presentation.theme.*

@Composable
fun TaskStatusBadge(status: TaskStatus) {
    val (bgColor, textColor) = when (status) {
        TaskStatus.COMPLETED -> AccentEmerald.copy(alpha = 0.15f) to AccentEmerald
        TaskStatus.RUNNING -> PrimaryBlue.copy(alpha = 0.15f) to PrimaryBlueLight
        TaskStatus.PENDING -> AccentAmber.copy(alpha = 0.15f) to AccentAmber
        TaskStatus.FAILED -> AccentRose.copy(alpha = 0.15f) to AccentRose
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

@Composable
fun StepStatusBadge(status: StepStatus) {
    val (bgColor, textColor) = when (status) {
        StepStatus.SUCCESS -> AccentEmerald.copy(alpha = 0.15f) to AccentEmerald
        StepStatus.RUNNING -> AccentCyan.copy(alpha = 0.15f) to AccentCyan
        StepStatus.WAITING -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        StepStatus.FAILED -> AccentRose.copy(alpha = 0.15f) to AccentRose
        StepStatus.SKIPPED -> AccentAmber.copy(alpha = 0.15f) to AccentAmber
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor
        )
    }
}
