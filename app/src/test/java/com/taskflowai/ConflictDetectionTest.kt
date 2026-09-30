package com.taskflowai

import com.taskflowai.domain.model.CalendarEvent
import com.taskflowai.domain.model.Conflict
import com.taskflowai.domain.model.ConflictType
import com.taskflowai.domain.repository.CalendarRepository
import com.taskflowai.domain.usecase.DetectCalendarConflictsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ConflictDetectionTest {

    @Test
    fun `test detect conflict when event overlaps`() = runBlocking {
        val calendarRepo = mockk<CalendarRepository>()
        val start = 1727170000000L
        val end = start + (60 * 60 * 1000L)

        val existingEvent = CalendarEvent(
            id = "evt_existing",
            title = "Existing Sync",
            startTime = start,
            endTime = end
        )
        val expectedConflict = Conflict(
            conflictingEvent = existingEvent,
            conflictType = ConflictType.EXACT_OVERLAP,
            message = "Conflict with Existing Sync"
        )

        coEvery { calendarRepo.checkConflict(start, end, null) } returns expectedConflict

        val useCase = DetectCalendarConflictsUseCase(calendarRepo)
        val conflict = useCase(start, end)

        assertNotNull(conflict)
        assertEquals("Existing Sync", conflict?.conflictingEvent?.title)
    }

    @Test
    fun `test no conflict when slot is free`() = runBlocking {
        val calendarRepo = mockk<CalendarRepository>()
        val start = 1727170000000L
        val end = start + (60 * 60 * 1000L)

        coEvery { calendarRepo.checkConflict(start, end, null) } returns null

        val useCase = DetectCalendarConflictsUseCase(calendarRepo)
        val conflict = useCase(start, end)

        assertNull(conflict)
    }
}
