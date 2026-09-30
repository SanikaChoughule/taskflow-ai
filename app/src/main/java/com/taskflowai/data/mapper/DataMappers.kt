package com.taskflowai.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taskflowai.data.database.entity.*
import com.taskflowai.domain.model.*

object DataMappers {
    private val gson = Gson()

    fun TaskEntity.toDomain(subTasks: List<SubTask> = emptyList()): Task {
        return Task(
            id = id,
            userId = userId,
            title = title,
            description = description,
            originalCommand = originalCommand,
            status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.PENDING),
            priority = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.MEDIUM),
            createdAt = createdAt,
            completedAt = completedAt,
            subTasks = subTasks,
            linkedCalendarEventId = linkedCalendarEventId,
            linkedReminderId = linkedReminderId,
            isUndoable = isUndoable,
            errorMessage = errorMessage
        )
    }

    fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            userId = userId,
            title = title,
            description = description,
            originalCommand = originalCommand,
            status = status.name,
            priority = priority.name,
            createdAt = createdAt,
            completedAt = completedAt,
            linkedCalendarEventId = linkedCalendarEventId,
            linkedReminderId = linkedReminderId,
            isUndoable = isUndoable,
            errorMessage = errorMessage
        )
    }

    fun SubTaskEntity.toDomain(): SubTask {
        return SubTask(
            id = id,
            taskId = taskId,
            stepOrder = stepOrder,
            title = title,
            description = description,
            status = runCatching { StepStatus.valueOf(status) }.getOrDefault(StepStatus.WAITING),
            executedAt = executedAt,
            errorMessage = errorMessage
        )
    }

    fun SubTask.toEntity(): SubTaskEntity {
        return SubTaskEntity(
            id = id,
            taskId = taskId,
            stepOrder = stepOrder,
            title = title,
            description = description,
            status = status.name,
            executedAt = executedAt,
            errorMessage = errorMessage
        )
    }

    fun ActionLogEntity.toDomain(): ActionLog {
        val type = object : TypeToken<Map<String, String>>() {}.type
        val metadata: Map<String, String> = runCatching {
            gson.fromJson<Map<String, String>>(metadataJson, type)
        }.getOrNull() ?: emptyMap()

        return ActionLog(
            id = id,
            taskId = taskId,
            timestamp = timestamp,
            actionType = runCatching { ActionType.valueOf(actionType) }.getOrDefault(ActionType.COMMAND_RECEIVED),
            status = runCatching { ActionStatus.valueOf(status) }.getOrDefault(ActionStatus.INFO),
            description = description,
            metadata = metadata
        )
    }

    fun ActionLog.toEntity(): ActionLogEntity {
        return ActionLogEntity(
            id = id,
            taskId = taskId,
            timestamp = timestamp,
            actionType = actionType.name,
            status = status.name,
            description = description,
            metadataJson = gson.toJson(metadata)
        )
    }

    fun CalendarEventEntity.toDomain(): CalendarEvent {
        val type = object : TypeToken<List<String>>() {}.type
        val attendees: List<String> = runCatching {
            gson.fromJson<List<String>>(attendeesJson, type)
        }.getOrNull() ?: emptyList()

        return CalendarEvent(
            id = id,
            title = title,
            description = description,
            location = location,
            startTime = startTime,
            endTime = endTime,
            attendees = attendees,
            calendarName = calendarName,
            colorTag = colorTag,
            isAllDay = isAllDay,
            googleEventId = googleEventId
        )
    }

    fun CalendarEvent.toEntity(): CalendarEventEntity {
        return CalendarEventEntity(
            id = id,
            title = title,
            description = description,
            location = location,
            startTime = startTime,
            endTime = endTime,
            attendeesJson = gson.toJson(attendees),
            calendarName = calendarName,
            colorTag = colorTag,
            isAllDay = isAllDay,
            googleEventId = googleEventId
        )
    }

    fun ReminderEntity.toDomain(): Reminder {
        val timing = when (timingMinutesBefore) {
            10 -> ReminderTiming.MINUTES_10
            15 -> ReminderTiming.MINUTES_15
            30 -> ReminderTiming.MINUTES_30
            60 -> ReminderTiming.HOURS_1
            1440 -> ReminderTiming.DAYS_1
            else -> ReminderTiming.CUSTOM
        }

        return Reminder(
            id = id,
            taskId = taskId,
            calendarEventId = calendarEventId,
            title = title,
            description = description,
            triggerTime = triggerTime,
            timing = timing,
            customMinutesBefore = if (timing == ReminderTiming.CUSTOM) timingMinutesBefore else null,
            status = runCatching { ReminderStatus.valueOf(status) }.getOrDefault(ReminderStatus.SCHEDULED),
            createdAt = createdAt
        )
    }

    fun Reminder.toEntity(): ReminderEntity {
        val minutes = customMinutesBefore ?: timing.minutesBefore
        return ReminderEntity(
            id = id,
            taskId = taskId,
            calendarEventId = calendarEventId,
            title = title,
            description = description,
            triggerTime = triggerTime,
            timingMinutesBefore = minutes,
            status = status.name,
            createdAt = createdAt
        )
    }

    fun SuggestionEntity.toDomain(): Suggestion {
        return Suggestion(
            id = id,
            title = title,
            message = message,
            actionCommand = actionCommand,
            type = runCatching { SuggestionType.valueOf(type) }.getOrDefault(SuggestionType.PRE_MEETING_REMINDER),
            isDismissed = isDismissed,
            createdAt = createdAt
        )
    }

    fun Suggestion.toEntity(): SuggestionEntity {
        return SuggestionEntity(
            id = id,
            title = title,
            message = message,
            actionCommand = actionCommand,
            type = type.name,
            isDismissed = isDismissed,
            createdAt = createdAt
        )
    }

    fun UserEntity.toDomain(): User {
        return User(
            id = id,
            name = name,
            email = email,
            photoUrl = photoUrl,
            isGoogleConnected = isGoogleConnected,
            isCalendarConnected = isCalendarConnected,
            lastLoginAt = lastLoginAt
        )
    }

    fun User.toEntity(): UserEntity {
        return UserEntity(
            id = id,
            name = name,
            email = email,
            photoUrl = photoUrl,
            isGoogleConnected = isGoogleConnected,
            isCalendarConnected = isCalendarConnected,
            lastLoginAt = lastLoginAt
        )
    }
}
