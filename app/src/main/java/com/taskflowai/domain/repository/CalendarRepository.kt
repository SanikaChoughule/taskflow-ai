package com.taskflowai.domain.repository

import com.taskflowai.domain.model.CalendarEvent
import com.taskflowai.domain.model.Conflict
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun getEvents(): Flow<List<CalendarEvent>>
    fun getEventsForRange(startTime: Long, endTime: Long): Flow<List<CalendarEvent>>
    suspend fun getEventById(id: String): CalendarEvent?
    suspend fun createEvent(event: CalendarEvent): CalendarEvent
    suspend fun updateEvent(event: CalendarEvent): CalendarEvent
    suspend fun deleteEvent(eventId: String): Boolean
    suspend fun checkConflict(startTime: Long, endTime: Long, excludeEventId: String? = null): Conflict?
    suspend fun findNextFreeSlot(startTime: Long, durationMinutes: Int): Long?
}
