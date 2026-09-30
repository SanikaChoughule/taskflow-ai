package com.taskflowai.domain.usecase

import com.taskflowai.domain.model.Conflict
import com.taskflowai.domain.repository.CalendarRepository

class DetectCalendarConflictsUseCase(
    private val calendarRepository: CalendarRepository
) {
    suspend operator fun invoke(startTime: Long, endTime: Long, excludeEventId: String? = null): Conflict? {
        return calendarRepository.checkConflict(startTime, endTime, excludeEventId)
    }
}
