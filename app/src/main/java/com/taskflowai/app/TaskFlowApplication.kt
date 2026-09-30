package com.taskflowai.app

import android.app.Application
import com.taskflowai.domain.model.CalendarEvent
import com.taskflowai.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class TaskFlowApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Seed initial data for a realistic first launch experience
        CoroutineScope(Dispatchers.IO).launch {
            seedSampleDataIfEmpty()
        }
    }

    private suspend fun seedSampleDataIfEmpty() {
        val events = container.calendarRepository.getEvents().first()
        if (events.isNotEmpty()) return
        // If calendar has no events, seed a few realistic appointments for today and tomorrow
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 11)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val today11am = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 14)
        val today2pm = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        val tomorrow10am = cal.timeInMillis

        container.calendarRepository.createEvent(
            CalendarEvent(
                id = "evt_seed_1",
                title = "Design Sync with Product Team",
                description = "Sprint planning and UX review",
                startTime = today11am,
                endTime = today11am + (45 * 60 * 1000L),
                attendees = listOf("Sarah", "Michael"),
                calendarName = "Work Calendar",
                colorTag = "#3B82F6"
            )
        )

        container.calendarRepository.createEvent(
            CalendarEvent(
                id = "evt_seed_2",
                title = "Client Presentation: TaskFlow Architecture",
                description = "Demonstration of AI safety pipeline",
                startTime = today2pm,
                endTime = today2pm + (60 * 60 * 1000L),
                attendees = listOf("Rahul", "Elena"),
                calendarName = "Work Calendar",
                colorTag = "#8B5CF6"
            )
        )

        container.calendarRepository.createEvent(
            CalendarEvent(
                id = "evt_seed_3",
                title = "1-on-1 Catchup with Rahul",
                description = "Quarterly roadmap review",
                startTime = tomorrow10am,
                endTime = tomorrow10am + (30 * 60 * 1000L),
                attendees = listOf("Rahul"),
                calendarName = "Work Calendar",
                colorTag = "#06B6D4"
            )
        )

        // Seed suggestions
        container.suggestionRepository.refreshSuggestions()
    }
}
