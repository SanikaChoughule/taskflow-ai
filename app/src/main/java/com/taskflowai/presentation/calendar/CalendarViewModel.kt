package com.taskflowai.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.domain.model.CalendarEvent
import com.taskflowai.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CalendarTab {
    TODAY,
    TOMORROW,
    UPCOMING
}

enum class CalendarViewMode {
    AGENDA,
    DAY
}

class CalendarViewModel(
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    val allEvents: StateFlow<List<CalendarEvent>> = calendarRepository.getEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTab = MutableStateFlow(CalendarTab.TODAY)
    val selectedTab: StateFlow<CalendarTab> = _selectedTab.asStateFlow()

    private val _viewMode = MutableStateFlow(CalendarViewMode.AGENDA)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()

    private val _selectedEvent = MutableStateFlow<CalendarEvent?>(null)
    val selectedEvent: StateFlow<CalendarEvent?> = _selectedEvent.asStateFlow()

    fun selectTab(tab: CalendarTab) {
        _selectedTab.value = tab
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == CalendarViewMode.AGENDA) CalendarViewMode.DAY else CalendarViewMode.AGENDA
    }

    fun selectEvent(event: CalendarEvent?) {
        _selectedEvent.value = event
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            calendarRepository.deleteEvent(eventId)
            _selectedEvent.value = null
        }
    }
}
