package com.taskflowai.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.taskflowai.domain.model.CalendarEvent
import com.taskflowai.presentation.components.ConfirmationDialog
import com.taskflowai.presentation.components.EmptyStateView
import com.taskflowai.presentation.theme.AccentRose
import com.taskflowai.presentation.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    navController: NavController
) {
    val events by viewModel.allEvents.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val selectedEvent by viewModel.selectedEvent.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfToday = cal.timeInMillis

    cal.add(Calendar.DAY_OF_YEAR, 1)
    val startOfTomorrow = cal.timeInMillis

    cal.add(Calendar.DAY_OF_YEAR, 1)
    val startOfAfterTomorrow = cal.timeInMillis

    val filteredEvents = events.filter { event ->
        when (selectedTab) {
            CalendarTab.TODAY -> event.startTime in startOfToday until startOfTomorrow
            CalendarTab.TOMORROW -> event.startTime in startOfTomorrow until startOfAfterTomorrow
            CalendarTab.UPCOMING -> event.startTime >= startOfAfterTomorrow
        }
    }

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Google Calendar") },
                actions = {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (viewMode == CalendarViewMode.AGENDA) Icons.Default.ViewDay else Icons.Default.ViewAgenda,
                            contentDescription = "Toggle View"
                        )
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
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedTab == CalendarTab.TODAY,
                    onClick = { viewModel.selectTab(CalendarTab.TODAY) },
                    text = { Text("Today") }
                )
                Tab(
                    selected = selectedTab == CalendarTab.TOMORROW,
                    onClick = { viewModel.selectTab(CalendarTab.TOMORROW) },
                    text = { Text("Tomorrow") }
                )
                Tab(
                    selected = selectedTab == CalendarTab.UPCOMING,
                    onClick = { viewModel.selectTab(CalendarTab.UPCOMING) },
                    text = { Text("Upcoming") }
                )
            }

            if (filteredEvents.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.EventAvailable,
                    title = "No Events Scheduled",
                    description = "You have no appointments on this day. Use TaskFlow AI to schedule one!"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEvents) { event ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectEvent(event) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val eventColor = runCatching { Color(android.graphics.Color.parseColor(event.colorTag)) }.getOrDefault(PrimaryBlue)

                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(eventColor)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (event.attendees.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Attendees: ${event.attendees.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Event Detail Bottom Sheet
        selectedEvent?.let { event ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.selectEvent(null) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dateFormat.format(Date(event.startTime)), style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${timeFormat.format(Date(event.startTime))} to ${timeFormat.format(Date(event.endTime))}", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (event.description.isNotEmpty()) {
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }

            if (showDeleteConfirm) {
                ConfirmationDialog(
                    title = "Delete Calendar Event",
                    message = "Are you sure you want to remove \"${event.title}\" from Google Calendar?",
                    confirmButtonText = "Delete",
                    onConfirm = {
                        showDeleteConfirm = false
                        viewModel.deleteEvent(event.id)
                    },
                    onDismiss = { showDeleteConfirm = false }
                )
            }
        }
    }
}
