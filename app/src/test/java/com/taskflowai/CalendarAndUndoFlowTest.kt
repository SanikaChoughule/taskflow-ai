package com.taskflowai

import com.taskflowai.calendar.MockCalendarService
import com.taskflowai.domain.model.ScheduleHistoryItem
import com.taskflowai.domain.repository.ScheduleHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CalendarAndUndoFlowTest {

    private lateinit var calendarService: MockCalendarService
    private lateinit var historyRepository: ScheduleHistoryRepository

    @Before
    fun setUp() {
        calendarService = MockCalendarService()
        historyRepository = mockk(relaxed = true)
    }

    @Test
    fun `test initial upcoming plans contain pre-seeded events`() = runBlocking {
        val initialEvents = calendarService.getUpcomingEvents(3).first()
        assertTrue(initialEvents.isNotEmpty())
        assertEquals(2, initialEvents.size)
        assertEquals("Team Sync", initialEvents[0].title)
        assertEquals("Project Discussion", initialEvents[1].title)
    }

    @Test
    fun `test creating meeting adds to upcoming plans and generates calendar ID`() = runBlocking {
        val now = System.currentTimeMillis()
        val result = calendarService.createEvent(
            title = "Sprint Planning",
            startTime = now + 3600000,
            endTime = now + 7200000,
            attendeeName = "Alex",
            attendeeEmail = "alex@gmail.com",
            reminderMinutes = 10
        )

        assertTrue(result.isSuccess)
        val event = result.getOrThrow()
        assertTrue(event.id.startsWith("gcal_"))
        assertEquals("Sprint Planning", event.title)

        val updatedEvents = calendarService.getUpcomingEvents(3).first()
        assertTrue(updatedEvents.any { it.id == event.id })
    }

    @Test
    fun `test undo flow deletes event from calendar and records UNDONE in history`() = runBlocking {
        val now = System.currentTimeMillis()
        val created = calendarService.createEvent(
            title = "Quick Sync",
            startTime = now + 1000000,
            endTime = now + 2000000
        ).getOrThrow()

        // 1. Record SCHEDULED
        val historyItem = ScheduleHistoryItem(
            id = UUID.randomUUID().toString(),
            title = created.title,
            scheduledDate = "Tomorrow",
            startTimeFormatted = "10:00 AM",
            endTimeFormatted = "10:30 AM",
            startTimeMs = created.startTime,
            endTimeMs = created.endTime,
            action = "SCHEDULED",
            actionTime = System.currentTimeMillis(),
            calendarEventId = created.id
        )
        historyRepository.recordScheduled(historyItem)
        coVerify { historyRepository.recordScheduled(any()) }

        // 2. Perform Undo (deletes event from Google Calendar)
        val deleteResult = calendarService.deleteEvent(created.id)
        assertTrue(deleteResult.isSuccess)

        // 3. Update history to UNDONE
        historyRepository.recordUndone(created.id)
        coVerify { historyRepository.recordUndone(created.id) }

        // 4. Verify event is deleted and no longer in Upcoming Plans
        val remainingEvents = calendarService.getUpcomingEvents(3).first()
        assertFalse(remainingEvents.any { it.id == created.id })
    }
}
