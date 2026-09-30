package com.taskflowai

import com.taskflowai.ai.LocalRuleBasedAIServiceImpl
import com.taskflowai.domain.model.AIIntent
import com.taskflowai.domain.repository.CalendarRepository
import com.taskflowai.domain.usecase.CommandValidationResult
import com.taskflowai.domain.usecase.ProcessCommandUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProcessCommandUseCaseTest {

    private lateinit var aiService: LocalRuleBasedAIServiceImpl
    private lateinit var calendarRepository: CalendarRepository
    private lateinit var useCase: ProcessCommandUseCase

    @Before
    fun setUp() {
        aiService = LocalRuleBasedAIServiceImpl()
        calendarRepository = mockk(relaxed = true)
        useCase = ProcessCommandUseCase(aiService, calendarRepository)
    }

    @Test
    fun `test schedule meeting with Rahul parses intent participant and reminder`() = runBlocking {
        val command = "Schedule a meeting with Rahul tomorrow at 3 PM and remind me 30 minutes before."
        coEvery { calendarRepository.checkConflict(any(), any()) } returns null

        val result = useCase(command)

        assertTrue(result is CommandValidationResult.Valid)
        val valid = result as CommandValidationResult.Valid

        assertEquals(AIIntent.SCHEDULE_EVENT, valid.command.intent)
        assertEquals("Rahul", valid.command.entities.participant)
        assertEquals(30, valid.command.entities.reminderMinutesBefore)
        assertNotNull(valid.command.entities.targetTimestamp)
        assertEquals(5, valid.plan.steps.size)
    }

    @Test
    fun `test reminder intent extracts title and target timing`() = runBlocking {
        val command = "Remind me to call Mom at 7 PM"

        val result = useCase(command)

        assertTrue(result is CommandValidationResult.Valid)
        val valid = result as CommandValidationResult.Valid

        assertEquals(AIIntent.CREATE_REMINDER, valid.command.intent)
        assertTrue(valid.command.entities.title.contains("Call Mom", ignoreCase = true))
    }

    @Test
    fun `test cancel meeting triggers destructive plan with high risk`() = runBlocking {
        val command = "Cancel my meeting with Rahul"

        val result = useCase(command)

        assertTrue(result is CommandValidationResult.Valid)
        val valid = result as CommandValidationResult.Valid

        assertEquals(AIIntent.CANCEL_EVENT, valid.command.intent)
        assertTrue(valid.plan.requiresConfirmation)
    }

    @Test
    fun `test empty command returns error`() = runBlocking {
        val result = useCase("   ")
        assertTrue(result is CommandValidationResult.Error)
    }
}
