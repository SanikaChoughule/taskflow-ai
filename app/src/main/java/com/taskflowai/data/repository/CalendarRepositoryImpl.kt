package com.taskflowai.data.repository

import com.taskflowai.data.database.dao.CalendarEventDao
import com.taskflowai.data.mapper.DataMappers.toDomain
import com.taskflowai.data.mapper.DataMappers.toEntity
import com.taskflowai.domain.model.CalendarEvent
import com.taskflowai.domain.model.Conflict
import com.taskflowai.domain.model.ConflictType
import com.taskflowai.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarRepositoryImpl(
    private val calendarEventDao: CalendarEventDao
) : CalendarRepository {

    override fun getEvents(): Flow<List<CalendarEvent>> {
        return calendarEventDao.getAllEvents().map { list -> list.map { it.toDomain() } }
    }

    override fun getEventsForRange(startTime: Long, endTime: Long): Flow<List<CalendarEvent>> {
        return calendarEventDao.getEventsInRange(startTime, endTime).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getEventById(id: String): CalendarEvent? {
        return calendarEventDao.getEventById(id)?.toDomain()
    }

    override suspend fun createEvent(event: CalendarEvent): CalendarEvent {
        calendarEventDao.insertEvent(event.toEntity())
        return event
    }

    override suspend fun updateEvent(event: CalendarEvent): CalendarEvent {
        calendarEventDao.updateEvent(event.toEntity())
        return event
    }

    override suspend fun deleteEvent(eventId: String): Boolean {
        calendarEventDao.deleteEventById(eventId)
        return true
    }

    override suspend fun checkConflict(startTime: Long, endTime: Long, excludeEventId: String?): Conflict? {
        val events = calendarEventDao.getEventsInRange(startTime, endTime).firstOrNull() ?: emptyList()
        val conflicting = events.firstOrNull { it.id != excludeEventId } ?: return null

        val domain = conflicting.toDomain()
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        val msg = "Conflicts with \"${domain.title}\" (${timeFmt.format(Date(domain.startTime))} - ${timeFmt.format(Date(domain.endTime))})"

        return Conflict(
            conflictingEvent = domain,
            conflictType = ConflictType.PARTIAL_OVERLAP,
            message = msg
        )
    }

    override suspend fun findNextFreeSlot(startTime: Long, durationMinutes: Int): Long? {
        val durationMs = durationMinutes * 60 * 1000L
        val searchWindowEnd = startTime + (7 * 86400000L) // Next 7 days
        val events = calendarEventDao.getEventsInRange(startTime, searchWindowEnd).firstOrNull() ?: emptyList()

        var testStart = startTime
        for (event in events) {
            val eventDomain = event.toDomain()
            if (testStart + durationMs <= eventDomain.startTime) {
                return testStart
            }
            if (testStart < eventDomain.endTime) {
                testStart = eventDomain.endTime + (15 * 60 * 1000L) // 15 min buffer
            }
        }
        return testStart
    }
}
