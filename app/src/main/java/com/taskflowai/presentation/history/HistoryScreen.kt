package com.taskflowai.presentation.history

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.taskflowai.domain.model.ScheduleHistoryItem
import com.taskflowai.presentation.components.EmptyStateView
import com.taskflowai.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    navController: NavController
) {
    val historyItems by viewModel.historyItems.collectAsState()

    // Partition into Today and Earlier
    val now = Calendar.getInstance()
    val todayItems = mutableListOf<ScheduleHistoryItem>()
    val earlierItems = mutableListOf<ScheduleHistoryItem>()

    historyItems.forEach { item ->
        val itemCal = Calendar.getInstance().apply { timeInMillis = item.actionTime }
        if (isSameDay(itemCal, now)) {
            todayItems.add(item)
        } else {
            earlierItems.add(item)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (historyItems.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.History,
                title = "No Scheduling History",
                description = "Meetings you schedule and undo with TaskFlow AI will be recorded here.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (todayItems.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Today")
                    }
                    items(todayItems, key = { it.id }) { item ->
                        HistoryRecordCard(item = item)
                    }
                }

                if (earlierItems.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(title = "Earlier")
                    }
                    items(earlierItems, key = { it.id }) { item ->
                        HistoryRecordCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun HistoryRecordCard(item: ScheduleHistoryItem) {
    val isUndone = item.action.equals("UNDONE", ignoreCase = true)
    val timeFmt = SimpleDateFormat("h:mm a", Locale.US)
    val actionTimeFormatted = timeFmt.format(Date(item.actionTime))

    val actionText = if (isUndone) {
        "Undone today at $actionTimeFormatted"
    } else {
        "Today • $actionTimeFormatted"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isUndone) AccentAmber.copy(alpha = 0.15f) else AccentEmerald.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isUndone) Icons.Default.Undo else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isUndone) AccentAmber else AccentEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isUndone) "UNDONE" else "SCHEDULED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (isUndone) AccentAmber else AccentEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Scheduled Date & Time
            Text(
                text = "${item.scheduledDate} • ${item.startTimeFormatted} – ${item.endTimeFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.attendee != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Attendee: ${item.attendee}${if (item.attendeeEmail != null) " (${item.attendeeEmail})" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = if (isUndone) AccentAmber.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
