package com.taskflowai.ai

import com.taskflowai.domain.model.*
import java.util.Calendar
import java.util.UUID
import java.util.regex.Pattern

class LocalRuleBasedAIServiceImpl : AIService {

    override suspend fun analyzeCommand(rawText: String): Result<AICommand> {
        val lower = rawText.lowercase().trim()

        val intent = when {
            lower.startsWith("cancel") || lower.contains("cancel my") || lower.contains("delete meeting") -> {
                AIIntent.CANCEL_EVENT
            }
            lower.startsWith("move") || lower.contains("reschedule") || lower.contains("shift") -> {
                AIIntent.RESCHEDULE_EVENT
            }
            lower.startsWith("find") || lower.contains("free slot") || lower.contains("available time") -> {
                AIIntent.FIND_FREE_SLOT
            }
            lower.startsWith("show") || lower.startsWith("what is") || lower.contains("agenda") || lower.contains("list meetings") -> {
                AIIntent.QUERY_SCHEDULE
            }
            lower.startsWith("schedule") || lower.startsWith("book") || lower.startsWith("set up") -> {
                AIIntent.SCHEDULE_EVENT
            }
            lower.startsWith("remind") || lower.contains("remind me to") || lower.contains("create a reminder") -> {
                AIIntent.CREATE_REMINDER
            }
            lower.contains("meeting") || lower.contains("schedule") -> {
                AIIntent.SCHEDULE_EVENT
            }
            lower.contains("remind") || lower.contains("reminder") -> {
                AIIntent.CREATE_REMINDER
            }
            else -> AIIntent.UNKNOWN
        }

        val participant = extractParticipant(rawText)
        val targetTimestamp = parseDateTime(rawText)
        val reminderMinutes = extractReminderMinutes(rawText)
        val title = extractTitle(rawText, intent, participant)

        val entities = ParsedEntities(
            title = title,
            participant = participant,
            dateString = extractDateString(rawText),
            timeString = extractTimeString(rawText),
            targetTimestamp = targetTimestamp,
            durationMinutes = 60,
            reminderMinutesBefore = reminderMinutes,
            rawTargetMeeting = if (intent == AIIntent.CANCEL_EVENT || intent == AIIntent.RESCHEDULE_EVENT) title else null
        )

        val command = AICommand(
            id = UUID.randomUUID().toString(),
            rawText = rawText,
            intent = intent,
            entities = entities,
            confidence = if (intent != AIIntent.UNKNOWN) 0.95f else 0.2f,
            explanation = "TaskFlow AI identified action: ${intent.name.replace('_', ' ').lowercase()}"
        )

        return Result.success(command)
    }

    override suspend fun createTaskPlan(command: AICommand, existingConflict: Conflict?): Result<TaskPlan> {
        val steps = mutableListOf<TaskStep>()
        var risk = RiskLevel.LOW
        var requiresConfirmation = false
        var confirmationPrompt: String? = null

        when (command.intent) {
            AIIntent.SCHEDULE_EVENT -> {
                steps.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        stepOrder = 1,
                        title = "Resolve Participant",
                        description = "Verify contact details for ${command.entities.participant ?: "attendees"}"
                    )
                )
                steps.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        stepOrder = 2,
                        title = "Check Calendar Availability",
                        description = "Scan Google Calendar for overlapping appointments"
                    )
                )
                steps.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        stepOrder = 3,
                        title = "Create Calendar Event",
                        description = "Schedule \"${command.entities.title}\" on Primary Calendar"
                    )
                )
                if (command.entities.reminderMinutesBefore != null) {
                    steps.add(
                        TaskStep(
                            id = UUID.randomUUID().toString(),
                            stepOrder = 4,
                            title = "Configure Reminder",
                            description = "Set notification alert ${command.entities.reminderMinutesBefore} minutes prior"
                        )
                    )
                }
                steps.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        stepOrder = if (command.entities.reminderMinutesBefore != null) 5 else 4,
                        title = "Audit Trail & Confirmation",
                        description = "Record action logs and enable instant undo capability"
                    )
                )

                if (existingConflict != null) {
                    risk = RiskLevel.HIGH
                    requiresConfirmation = true
                    confirmationPrompt = "Conflict detected with \"${existingConflict.conflictingEvent.title}\". Do you want to schedule anyway?"
                }
            }
            AIIntent.CREATE_REMINDER -> {
                steps.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        stepOrder = 1,
                        title = "Schedule Notification",
                        description = "Queue WorkManager trigger for \"${command.entities.title}\""
                    )
                )
                steps.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        stepOrder = 2,
                        title = "Save Action Trail",
                        description = "Persist reminder and enable one-tap cancellation"
                    )
                )
            }
            AIIntent.CANCEL_EVENT -> {
                risk = RiskLevel.HIGH
                requiresConfirmation = true
                confirmationPrompt = "Are you sure you want to cancel \"${command.entities.title}\"?"
                steps.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        stepOrder = 1,
                        title = "Locate Event",
                        description = "Find matching calendar event for ${command.entities.title}"
                    )
                )
                steps.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        stepOrder = 2,
                        title = "Delete Event",
                        description = "Remove event from Google Calendar",
                        isDestructive = true
                    )
                )
            }
            AIIntent.RESCHEDULE_EVENT -> {
                steps.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        stepOrder = 1,
                        title = "Check New Slot Availability",
                        description = "Ensure new requested time has no conflict"
                    )
                )
                steps.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        stepOrder = 2,
                        title = "Update Calendar Event",
                        description = "Shift event time to ${command.entities.timeString ?: "new time"}"
                    )
                )
            }
            AIIntent.QUERY_SCHEDULE, AIIntent.FIND_FREE_SLOT -> {
                steps.add(
                    TaskStep(
                        id = UUID.randomUUID().toString(),
                        stepOrder = 1,
                        title = "Query Calendar",
                        description = "Fetch events and analyze open time windows"
                    )
                )
            }
            AIIntent.UNKNOWN -> {
                return Result.failure(IllegalArgumentException("Unknown intent cannot be planned"))
            }
        }

        val plan = TaskPlan(
            planId = UUID.randomUUID().toString(),
            intent = command.intent,
            summary = command.entities.title.ifEmpty { command.rawText },
            entities = command.entities,
            steps = steps,
            riskLevel = risk,
            requiresConfirmation = requiresConfirmation,
            confirmationPrompt = confirmationPrompt,
            detectedConflict = existingConflict
        )

        return Result.success(plan)
    }

    override suspend fun generateSuggestions(): List<Suggestion> {
        return listOf(
            Suggestion(
                id = UUID.randomUUID().toString(),
                title = "Upcoming Meeting Alert",
                message = "You have 3 meetings tomorrow morning. Would you like a reminder before your first meeting?",
                actionCommand = "Remind me 15 minutes before my first meeting tomorrow",
                type = SuggestionType.PRE_MEETING_REMINDER
            ),
            Suggestion(
                id = UUID.randomUUID().toString(),
                title = "Focus Time Opportunity",
                message = "You have a free 45-minute slot tomorrow at 2:00 PM. Schedule a focus session?",
                actionCommand = "Schedule Focus Block tomorrow at 2 PM",
                type = SuggestionType.FREE_SLOT_AVAILABLE
            ),
            Suggestion(
                id = UUID.randomUUID().toString(),
                title = "Weekly Review",
                message = "Plan your weekly goals and wrap up pending tasks for Friday.",
                actionCommand = "Schedule Weekly Review Friday at 5 PM",
                type = SuggestionType.TASK_OPTIMIZATION
            )
        )
    }

    private fun extractParticipant(text: String): String? {
        val pattern = Pattern.compile("with\\s+([A-Z][a-zA-Z]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractTitle(text: String, intent: AIIntent, participant: String?): String {
        return when (intent) {
            AIIntent.SCHEDULE_EVENT -> {
                if (participant != null) "Meeting with $participant" else "Scheduled Meeting"
            }
            AIIntent.CREATE_REMINDER -> {
                val callPattern = Pattern.compile("remind me to\\s+(.+?)(?:\\s+at|\\s+tomorrow|\\s+next|$)", Pattern.CASE_INSENSITIVE)
                val matcher = callPattern.matcher(text)
                if (matcher.find()) matcher.group(1)?.replaceFirstChar { it.uppercase() } ?: "Reminder" else "Reminder: $text"
            }
            AIIntent.CANCEL_EVENT -> {
                if (participant != null) "Meeting with $participant" else "Meeting"
            }
            else -> text
        }
    }

    private fun extractReminderMinutes(text: String): Int? {
        val lower = text.lowercase()
        return when {
            lower.contains("10 minutes before") || lower.contains("10 min before") -> 10
            lower.contains("15 minutes before") || lower.contains("15 min before") -> 15
            lower.contains("30 minutes before") || lower.contains("30 min before") || lower.contains("half hour before") -> 30
            lower.contains("1 hour before") || lower.contains("an hour before") -> 60
            lower.contains("1 day before") || lower.contains("a day before") -> 1440
            else -> null
        }
    }

    private fun extractDateString(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("today") -> "Today"
            lower.contains("tomorrow") -> "Tomorrow"
            lower.contains("next monday") -> "Next Monday"
            lower.contains("friday") -> "Friday"
            else -> null
        }
    }

    private fun extractTimeString(text: String): String? {
        val pattern = Pattern.compile("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|AM|PM))")
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun parseDateTime(text: String): Long? {
        val cal = Calendar.getInstance()
        val lower = text.lowercase()

        // Days offset
        if (lower.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        } else if (lower.contains("next monday")) {
            cal.add(Calendar.DAY_OF_YEAR, 4) // Approximate next monday
        }

        // Time parsing
        val timePattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)", Pattern.CASE_INSENSITIVE)
        val matcher = timePattern.matcher(text)
        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: 9
            val minute = matcher.group(2)?.toIntOrNull() ?: 0
            val ampm = matcher.group(3)?.lowercase()

            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        // Fallback default 1 hour in future if no time found
        return if (lower.contains("tomorrow")) {
            cal.set(Calendar.HOUR_OF_DAY, 15) // default 3 PM tomorrow
            cal.set(Calendar.MINUTE, 0)
            cal.timeInMillis
        } else null
    }
}
