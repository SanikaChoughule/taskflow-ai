package com.taskflowai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskflowai.domain.model.ActionLog
import com.taskflowai.domain.model.ActionStatus
import com.taskflowai.presentation.theme.AccentAmber
import com.taskflowai.presentation.theme.AccentEmerald
import com.taskflowai.presentation.theme.AccentRose
import com.taskflowai.presentation.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActionTrailTimeline(
    actionLogs: List<ActionLog>,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(modifier = modifier.fillMaxWidth()) {
        actionLogs.forEachIndexed { index, log ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Time
                Text(
                    text = timeFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.width(64.dp)
                )

                // Timeline node & line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    val (icon, color) = when (log.status) {
                        ActionStatus.SUCCESS -> Icons.Default.Check to AccentEmerald
                        ActionStatus.WARNING -> Icons.Default.Warning to AccentAmber
                        ActionStatus.FAILED -> Icons.Default.Close to AccentRose
                        ActionStatus.INFO -> Icons.Default.Info to PrimaryBlue
                    }

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = log.status.name,
                            tint = color,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    if (index < actionLogs.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        )
                    }
                }

                // Description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (log.status == ActionStatus.SUCCESS) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
