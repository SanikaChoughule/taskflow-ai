package com.taskflowai.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.calendar.CalendarEventDto
import com.taskflowai.calendar.CalendarService
import com.taskflowai.calendar.ParsedScheduleCommand
import com.taskflowai.calendar.ScheduleCommandParser
import com.taskflowai.domain.model.CalendarEvent
import com.taskflowai.domain.model.ScheduleHistoryItem
import com.taskflowai.domain.repository.CalendarRepository
import com.taskflowai.domain.repository.ScheduleHistoryRepository
import com.taskflowai.domain.repository.UserRepository
import com.taskflowai.voice.VoiceRecognizerManager
import com.taskflowai.voice.VoiceState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ScheduledSuccessState(
    val eventId: String,
    val title: String,
    val dateLabel: String,
    val timeRangeLabel: String,
    val startTimeMs: Long,
    val reminderLabel: String,
    val secondsRemaining: Int = 8,
    val isUndoExpired: Boolean = false,
    val isUndone: Boolean = false
)

class HomeViewModel(
    private val calendarService: CalendarService,
    private val scheduleHistoryRepository: ScheduleHistoryRepository,
    private val voiceRecognizerManager: VoiceRecognizerManager,
    private val userRepository: UserRepository? = null,
    private val calendarRepository: CalendarRepository? = null
) : ViewModel() {

    val upcomingPlans: StateFlow<List<CalendarEventDto>> = calendarService.getUpcomingEvents(limit = 3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voiceState: StateFlow<VoiceState> = voiceRecognizerManager.voiceState

    private val _confirmationCommand = MutableStateFlow<ParsedScheduleCommand?>(null)
    val confirmationCommand: StateFlow<ParsedScheduleCommand?> = _confirmationCommand.asStateFlow()

    private val _successState = MutableStateFlow<ScheduledSuccessState?>(null)
    val successState: StateFlow<ScheduledSuccessState?> = _successState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    private val _isConnectingCalendar = MutableStateFlow(false)
    val isConnectingCalendar: StateFlow<Boolean> = _isConnectingCalendar.asStateFlow()

    private var undoTimerJob: Job? = null

    init {
        // Automatically sync current logged-in user to MySQL backend on launch
        viewModelScope.launch {
            userRepository?.getCurrentUser()?.let { user ->
                userRepository.saveUser(user)
            }
        }

        // Collect voice recognition results automatically
        viewModelScope.launch {
            voiceState.collect { state ->
                when (state) {
                    is VoiceState.Result -> {
                        if (state.recognizedText.isNotBlank()) {
                            processCommand(state.recognizedText)
                        } else {
                            _statusMessage.value = "I couldn't understand your voice command."
                        }
                        voiceRecognizerManager.reset()
                    }
                    is VoiceState.Error -> {
                        _statusMessage.value = "I couldn't understand your voice command."
                        voiceRecognizerManager.reset()
                    }
                    else -> {}
                }
            }
        }
    }

    fun startListening() {
        _permissionDenied.value = false
        voiceRecognizerManager.startListening()
    }

    fun stopListening() {
        voiceRecognizerManager.stopListening()
    }

    fun onPermissionDenied() {
        _permissionDenied.value = true
        voiceRecognizerManager.reset()
    }

    fun clearPermissionDenied() {
        _permissionDenied.value = false
    }

    fun processCommand(commandText: String) {
        val text = commandText.trim()
        if (text.isBlank()) return

        val parsed = ScheduleCommandParser.parse(text)
        _confirmationCommand.value = parsed
    }

    fun dismissConfirmation() {
        _confirmationCommand.value = null
    }

    fun confirmSchedule(command: ParsedScheduleCommand) {
        viewModelScope.launch {
            _confirmationCommand.value = null

            // Create event in Google Calendar (or MockCalendarService)
            val result = calendarService.createEvent(
                title = command.title,
                startTime = command.startTimeMs,
                endTime = command.endTimeMs,
                attendeeName = command.attendeeName,
                attendeeEmail = command.attendeeEmail,
                reminderMinutes = command.reminderMinutes,
                timeZone = command.timeZoneId.ifBlank { TimeZone.getDefault().id }
            )

            result.onSuccess { createdEvent ->
                // Sync to local Room database calendar repository
                if (calendarRepository != null) {
                    try {
                        val domainEvent = CalendarEvent(
                            id = createdEvent.id,
                            title = createdEvent.title,
                            description = "Scheduled via TaskFlow AI",
                            startTime = createdEvent.startTime,
                            endTime = createdEvent.endTime,
                            attendees = listOfNotNull(createdEvent.attendeeName, createdEvent.attendeeEmail),
                            calendarName = "Google Calendar",
                            colorTag = "#2563EB",
                            googleEventId = createdEvent.id
                        )
                        calendarRepository.createEvent(domainEvent)
                    } catch (_: Exception) {}
                }

                // Record in history as SCHEDULED
                val currentUserId = userRepository?.getCurrentUser()?.id ?: ""
                val historyItem = ScheduleHistoryItem(
                    id = UUID.randomUUID().toString(),
                    userId = currentUserId,
                    title = createdEvent.title,
                    scheduledDate = command.dateLabel,
                    startTimeFormatted = command.startFormatted,
                    endTimeFormatted = command.endFormatted,
                    startTimeMs = createdEvent.startTime,
                    endTimeMs = createdEvent.endTime,
                    attendee = command.attendeeName,
                    attendeeEmail = command.attendeeEmail,
                    action = "SCHEDULED",
                    actionTime = System.currentTimeMillis(),
                    calendarEventId = createdEvent.id
                )
                scheduleHistoryRepository.recordScheduled(historyItem)

                val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.US)
                val formattedDate = dateFmt.format(Date(createdEvent.startTime))

                val successObj = ScheduledSuccessState(
                    eventId = createdEvent.id,
                    title = createdEvent.title,
                    dateLabel = formattedDate,
                    timeRangeLabel = "${command.startFormatted}–${command.endFormatted}",
                    startTimeMs = createdEvent.startTime,
                    reminderLabel = "${createdEvent.reminderMinutes} min reminder",
                    secondsRemaining = 8,
                    isUndoExpired = false,
                    isUndone = false
                )
                _successState.value = successObj

                // Start 8-second countdown timer ONLY after event creation succeeds
                startUndoCountdown(createdEvent.id)
            }.onFailure { error ->
                _statusMessage.value = "Unable to schedule the meeting. Please try again."
            }
        }
    }

    private fun startUndoCountdown(eventId: String) {
        undoTimerJob?.cancel()
        undoTimerJob = viewModelScope.launch {
            for (sec in 8 downTo 1) {
                _successState.update { current ->
                    if (current?.eventId == eventId && !current.isUndone) {
                        current.copy(secondsRemaining = sec)
                    } else current
                }
                delay(1000)
            }
            _successState.update { current ->
                if (current?.eventId == eventId && !current.isUndone) {
                    current.copy(secondsRemaining = 0, isUndoExpired = true)
                } else current
            }
        }
    }

    fun triggerUndo() {
        val current = _successState.value ?: return
        if (current.isUndoExpired || current.isUndone) return

        undoTimerJob?.cancel()
        viewModelScope.launch {
            // Delete actual event from Google Calendar / Mock Calendar
            val deleteResult = calendarService.deleteEvent(current.eventId)

            deleteResult.onSuccess {
                // Update History to UNDONE
                scheduleHistoryRepository.recordUndone(current.eventId)

                _successState.update { it?.copy(isUndone = true) }
                _statusMessage.value = "Meeting undone"

                // Close success sheet after brief indication
                delay(1200)
                _successState.value = null
            }.onFailure { error ->
                _statusMessage.value = "Unable to undo the meeting. Please check Google Calendar and try again."
            }
        }
    }

    fun dismissSuccess() {
        undoTimerJob?.cancel()
        _successState.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun openGoogleCalendar(context: Context) {
        val startTime = _successState.value?.startTimeMs ?: System.currentTimeMillis()
        calendarService.openCalendarApp(context, startTime)
    }

    override fun onCleared() {
        super.onCleared()
        undoTimerJob?.cancel()
        voiceRecognizerManager.reset()
    }
}
