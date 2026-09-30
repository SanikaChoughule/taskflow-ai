package com.taskflowai.domain.usecase

import com.taskflowai.domain.model.ActionLog
import com.taskflowai.domain.model.ActionStatus
import com.taskflowai.domain.model.ActionType
import com.taskflowai.domain.model.UndoType
import com.taskflowai.domain.repository.ActionLogRepository
import com.taskflowai.domain.repository.CalendarRepository
import com.taskflowai.domain.repository.ReminderRepository
import com.taskflowai.domain.repository.TaskRepository
import com.taskflowai.domain.repository.UndoRepository
import java.util.UUID

sealed class UndoResult {
    data class Success(val message: String) : UndoResult()
    data class Failure(val error: String) : UndoResult()
    data object NothingToUndo : UndoResult()
}

class UndoActionUseCase(
    private val undoRepository: UndoRepository,
    private val calendarRepository: CalendarRepository,
    private val reminderRepository: ReminderRepository,
    private val taskRepository: TaskRepository,
    private val actionLogRepository: ActionLogRepository
) {
    suspend operator fun invoke(): UndoResult {
        val latestAction = undoRepository.popUndoAction() ?: return UndoResult.NothingToUndo

        return try {
            when (latestAction.undoType) {
                UndoType.DELETE_CREATED_CALENDAR_EVENT -> {
                    calendarRepository.deleteEvent(latestAction.targetId)
                }
                UndoType.CANCEL_CREATED_REMINDER -> {
                    reminderRepository.deleteReminder(latestAction.targetId)
                }
                UndoType.RESTORE_PREVIOUS_CALENDAR_EVENT -> {
                    // Restore previous event state if JSON payload was preserved
                }
            }

            // Update task status to mark undo
            val task = taskRepository.getTaskById(latestAction.taskId)
            if (task != null) {
                taskRepository.updateTask(task.copy(isUndoable = false))
            }

            // Log action undo
            actionLogRepository.insertActionLog(
                ActionLog(
                    id = UUID.randomUUID().toString(),
                    taskId = latestAction.taskId,
                    timestamp = System.currentTimeMillis(),
                    actionType = ActionType.ACTION_UNDONE,
                    status = ActionStatus.SUCCESS,
                    description = "Undone: ${latestAction.description}"
                )
            )

            UndoResult.Success("Action undone successfully: ${latestAction.description}")
        } catch (e: Exception) {
            UndoResult.Failure("Failed to undo action: ${e.message}")
        }
    }
}
