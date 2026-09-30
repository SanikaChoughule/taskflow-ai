package com.taskflowai

import com.taskflowai.domain.model.UndoAction
import com.taskflowai.domain.model.UndoType
import com.taskflowai.domain.repository.*
import com.taskflowai.domain.usecase.UndoActionUseCase
import com.taskflowai.domain.usecase.UndoResult
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class UndoActionTest {

    @Test
    fun `test undo reverses created calendar event`() = runBlocking {
        val undoRepo = mockk<UndoRepository>()
        val calendarRepo = mockk<CalendarRepository>(relaxed = true)
        val reminderRepo = mockk<ReminderRepository>(relaxed = true)
        val taskRepo = mockk<TaskRepository>(relaxed = true)
        val actionLogRepo = mockk<ActionLogRepository>(relaxed = true)

        val undoAction = UndoAction(
            id = "undo_1",
            taskId = "task_1",
            undoType = UndoType.DELETE_CREATED_CALENDAR_EVENT,
            description = "Delete event: Meeting with Rahul",
            targetId = "evt_123"
        )

        coEvery { undoRepo.popUndoAction() } returns undoAction
        coEvery { calendarRepo.deleteEvent("evt_123") } returns true

        val useCase = UndoActionUseCase(
            undoRepository = undoRepo,
            calendarRepository = calendarRepo,
            reminderRepository = reminderRepo,
            taskRepository = taskRepo,
            actionLogRepository = actionLogRepo
        )

        val result = useCase()

        assertTrue(result is UndoResult.Success)
        coVerify(exactly = 1) { calendarRepo.deleteEvent("evt_123") }
        coVerify(exactly = 1) { actionLogRepo.insertActionLog(any()) }
    }

    @Test
    fun `test undo returns NothingToUndo when stack is empty`() = runBlocking {
        val undoRepo = mockk<UndoRepository>()
        coEvery { undoRepo.popUndoAction() } returns null

        val useCase = UndoActionUseCase(
            undoRepository = undoRepo,
            calendarRepository = mockk(),
            reminderRepository = mockk(),
            taskRepository = mockk(),
            actionLogRepository = mockk()
        )

        val result = useCase()
        assertTrue(result is UndoResult.NothingToUndo)
    }
}
