package com.taskflowai.domain.usecase

import com.taskflowai.domain.model.*
import com.taskflowai.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class DashboardData(
    val user: User?,
    val recentTasks: List<Task>,
    val todayEvents: List<CalendarEvent>,
    val upcomingReminders: List<Reminder>,
    val suggestions: List<Suggestion>,
    val recentLogs: List<ActionLog>,
    val latestUndoAction: UndoAction?
)

class GetDashboardDataUseCase(
    private val userRepository: UserRepository,
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository,
    private val reminderRepository: ReminderRepository,
    private val suggestionRepository: SuggestionRepository,
    private val actionLogRepository: ActionLogRepository,
    private val undoRepository: UndoRepository
) {
    operator fun invoke(): Flow<DashboardData> {
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % 86400000L)
        val endOfDay = startOfDay + 86400000L

        val flow1 = combine(
            userRepository.getAuthState(),
            taskRepository.getRecentTasks(5),
            calendarRepository.getEventsForRange(startOfDay, endOfDay),
            reminderRepository.getUpcomingReminders()
        ) { authState, tasks, events, reminders ->
            val user = (authState as? AuthState.Authenticated)?.user
            DataPart1(user, tasks, events, reminders)
        }

        val flow2 = combine(
            suggestionRepository.getActiveSuggestions(),
            actionLogRepository.getRecentActionLogs(5),
            undoRepository.getLatestUndoAction()
        ) { suggestions, logs, undo ->
            DataPart2(suggestions, logs, undo)
        }

        return combine(flow1, flow2) { part1, part2 ->
            DashboardData(
                user = part1.user,
                recentTasks = part1.tasks,
                todayEvents = part1.events,
                upcomingReminders = part1.reminders,
                suggestions = part2.suggestions,
                recentLogs = part2.logs,
                latestUndoAction = part2.undo
            )
        }
    }
}

private data class DataPart1(
    val user: User?,
    val tasks: List<Task>,
    val events: List<CalendarEvent>,
    val reminders: List<Reminder>
)

private data class DataPart2(
    val suggestions: List<Suggestion>,
    val logs: List<ActionLog>,
    val undo: UndoAction?
)
