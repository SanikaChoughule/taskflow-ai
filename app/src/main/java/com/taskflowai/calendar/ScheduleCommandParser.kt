package com.taskflowai.calendar

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

data class ParsedScheduleCommand(
    val title: String,
    val dateLabel: String,
    val startFormatted: String,
    val endFormatted: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val attendeeName: String? = null,
    val attendeeEmail: String? = null,
    val reminderMinutes: Int = 10,
    val reminderLabel: String = "10 minutes before",
    val rawCommand: String,
    val dateIso: String = "",
    val startTime24: String = "",
    val endTime24: String = "",
    val timeZoneId: String = ""
)

object ScheduleCommandParser {

    private val EMAIL_PATTERN = Pattern.compile(
        "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})",
        Pattern.CASE_INSENSITIVE
    )

    // Duration: "for 30 minutes", "for 1 hour", "for 45 mins", "for an hour"
    private val DURATION_PATTERN = Pattern.compile(
        "(?:for\\s+)(\\d+|an?|half(?:\\s+an)?)\\s*(min|minute|minutes|mins|hour|hours|hr|hrs)?",
        Pattern.CASE_INSENSITIVE
    )

    // Reminder: "remind me 10 minutes before", "15 mins prior", etc.
    private val REMINDER_PATTERN = Pattern.compile(
        "(\\d+)\\s*(?:min|minute|minutes|hour|hours|hr|hrs)?\\s*(?:before|prior)",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(rawText: String, timeZone: TimeZone = TimeZone.getDefault()): ParsedScheduleCommand {
        val trimmed = rawText.trim().removeSuffix(".")
        val normalized = normalizeSpokenText(trimmed)

        // 1. Extract Attendee Email
        val emailMatcher = EMAIL_PATTERN.matcher(trimmed)
        val attendeeEmail = if (emailMatcher.find()) emailMatcher.group(1) else null

        // 2. Extract Attendee Name ("with Alex", "with Rahul Sharma", etc.)
        val attendeeName = extractAttendeeName(trimmed)

        // 3. Extract Reminder (default: 10 minutes before)
        val reminderMinutes = extractReminderMinutes(normalized)

        // 4. Extract Date
        val (baseCal, dateLabel, dateIso) = extractDate(normalized, timeZone)

        // 5. Extract Start & End Time (NEVER default to 10:00 AM if time was spoken!)
        val (startCal, endCal, startFormatted, endFormatted, start24, end24) = extractTime(
            normalized = normalized,
            baseCal = baseCal,
            timeZone = timeZone
        )

        // 6. Extract Meeting Title
        val title = extractTitle(trimmed, normalized, attendeeName)

        val reminderLabel = when {
            reminderMinutes >= 60 && reminderMinutes % 60 == 0 -> "${reminderMinutes / 60} hour before"
            else -> "$reminderMinutes minutes before"
        }

        return ParsedScheduleCommand(
            title = title,
            dateLabel = dateLabel,
            startFormatted = startFormatted,
            endFormatted = endFormatted,
            startTimeMs = startCal.timeInMillis,
            endTimeMs = endCal.timeInMillis,
            attendeeName = attendeeName,
            attendeeEmail = attendeeEmail,
            reminderMinutes = reminderMinutes,
            reminderLabel = reminderLabel,
            rawCommand = trimmed,
            dateIso = dateIso,
            startTime24 = start24,
            endTime24 = end24,
            timeZoneId = timeZone.id
        )
    }

    /**
     * Normalizes various speech-to-text transcriptions into clean, parseable tokens.
     */
    private fun normalizeSpokenText(input: String): String {
        var text = input.lowercase(Locale.ROOT)

        // Periods in AM/PM: "a.m." -> "am", "p.m." -> "pm"
        text = text.replace("a.m.", "am").replace("a. m.", "am")
        text = text.replace("p.m.", "pm").replace("p. m.", "pm")

        // Period separators in times: "8.00 am" -> "8:00 am", "8.45 pm" -> "8:45 pm"
        text = text.replace(Regex("(\\b\\d{1,2})\\.(\\d{2})\\s*(am|pm\\b)"), "$1:$2 $3")

        // Colon spacing: "8 : 30" -> "8:30"
        text = text.replace(Regex("(\\d{1,2})\\s*:\\s*(\\d{2})"), "$1:$2")

        // Phrases like "8 o'clock in the morning" -> "8 am", "8 in the morning" -> "8 am"
        text = text.replace(Regex("(\\b\\d{1,2}(?::\\d{2})?)\\s*(?:o'clock\\s+)?in the morning\\b"), "$1 am")
        text = text.replace(Regex("(\\b\\d{1,2}(?::\\d{2})?)\\s*(?:o'clock\\s+)?(?:in the afternoon|in the evening|at night)\\b"), "$1 pm")
        text = text.replace(Regex("(\\b\\d{1,2})\\s*o'clock\\b"), "$1")

        // Word numbers for hours
        val wordNumbers = mapOf(
            "twelve" to "12", "eleven" to "11", "ten" to "10",
            "nine" to "9", "eight" to "8", "seven" to "7",
            "six" to "6", "five" to "5", "four" to "4",
            "three" to "3", "two" to "2", "one" to "1"
        )
        for ((word, num) in wordNumbers) {
            // "eight forty five pm" -> "8 45 pm"
            text = text.replace(Regex("\\b$word\\s+forty[ -]?five\\s*(am|pm\\b)"), "$num:45 $1")
            text = text.replace(Regex("\\b$word\\s+thirty\\s*(am|pm\\b)"), "$num:30 $1")
            text = text.replace(Regex("\\b$word\\s+fifteen\\s*(am|pm\\b)"), "$num:15 $1")
            text = text.replace(Regex("\\b$word\\s*(am|pm\\b)"), "$num $1")
            text = text.replace(Regex("\\bat\\s+$word\\s*(?:o'clock\\s+)?in the morning\\b"), "at $num am")
            text = text.replace(Regex("\\bat\\s+$word\\s*(?:o'clock\\s+)?(?:in the evening|in the afternoon|at night)\\b"), "at $num pm")
            text = text.replace(Regex("\\bat\\s+$word\\b"), "at $num")
        }

        // "half past 8" -> "8:30", "half past eight" -> "8:30"
        for ((word, num) in wordNumbers) {
            text = text.replace(Regex("\\bhalf past $word\\b"), "$num:30")
        }
        text = text.replace(Regex("\\bhalf past (\\d{1,2})\\b"), "$1:30")

        return text
    }

    private fun extractAttendeeName(rawText: String): String? {
        val withPattern = Pattern.compile(
            "with\\s+([A-Za-z]+)(?:\\s+(?!(?:at|tomorrow|today|from|to|on|for|and|in)\\b)[A-Za-z]+)?",
            Pattern.CASE_INSENSITIVE
        )
        val withMatcher = withPattern.matcher(rawText)
        if (withMatcher.find()) {
            val candidate = withMatcher.group(1)?.trim()
            if (candidate != null &&
                !candidate.equals("at", ignoreCase = true) &&
                !candidate.contains("@") &&
                !candidate.equals("team", ignoreCase = true)
            ) {
                return candidate
            }
        }
        return null
    }

    private fun extractReminderMinutes(normalized: String): Int {
        val remMatcher = REMINDER_PATTERN.matcher(normalized)
        if (remMatcher.find()) {
            val amount = remMatcher.group(1)?.toIntOrNull() ?: 10
            val isHour = normalized.contains("hour before") || normalized.contains("hours before") || normalized.contains("hr before")
            return if (isHour) amount * 60 else amount
        }
        if (normalized.contains("half hour before") || normalized.contains("30 min before")) {
            return 30
        }
        return 10
    }

    private fun extractDate(
        normalized: String,
        timeZone: TimeZone
    ): Triple<Calendar, String, String> {
        val baseCal = Calendar.getInstance(timeZone)
        var dateLabel = "Today"

        // 1. Day after tomorrow (check before tomorrow!)
        if (normalized.contains("day after tomorrow")) {
            baseCal.add(Calendar.DAY_OF_YEAR, 2)
            dateLabel = "Day after tomorrow"
        }
        // 2. Tomorrow
        else if (normalized.contains("tomorrow")) {
            baseCal.add(Calendar.DAY_OF_YEAR, 1)
            dateLabel = "Tomorrow"
        }
        // 3. Specific calendar month & day (e.g., "September 25", "25 September", "Sep 25th")
        else {
            val specificDate = parseSpecificDate(normalized, baseCal)
            if (specificDate != null) {
                dateLabel = specificDate
            } else {
                // 4. Weekdays
                val weekdayFound = parseWeekday(normalized, baseCal)
                if (weekdayFound != null) {
                    dateLabel = weekdayFound
                } else {
                    dateLabel = "Today"
                }
            }
        }

        val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }
        val dateIso = isoFmt.format(baseCal.time)

        return Triple(baseCal, dateLabel, dateIso)
    }

    private fun parseSpecificDate(normalized: String, cal: Calendar): String? {
        val monthsRegex = "(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)"
        val pattern1 = Pattern.compile("\\b$monthsRegex\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b", Pattern.CASE_INSENSITIVE)
        val m1 = pattern1.matcher(normalized)

        val pattern2 = Pattern.compile("\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+(?:of\\s+)?$monthsRegex\\b", Pattern.CASE_INSENSITIVE)
        val m2 = pattern2.matcher(normalized)

        var monthName: String? = null
        var dayOfMonth: Int? = null

        if (m1.find()) {
            monthName = m1.group(1)
            dayOfMonth = m1.group(2)?.toIntOrNull()
        } else if (m2.find()) {
            dayOfMonth = m2.group(1)?.toIntOrNull()
            monthName = m2.group(2)
        }

        if (monthName != null && dayOfMonth != null && dayOfMonth in 1..31) {
            val monthIndex = parseMonthIndex(monthName)
            if (monthIndex != -1) {
                val currentYear = cal.get(Calendar.YEAR)
                cal.set(Calendar.MONTH, monthIndex)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                // If date in past this year, advance to next year
                val now = Calendar.getInstance(cal.timeZone)
                if (cal.before(now)) {
                    cal.set(Calendar.YEAR, currentYear + 1)
                }
                val fmt = SimpleDateFormat("MMM d", Locale.US).apply { timeZone = cal.timeZone }
                return fmt.format(cal.time)
            }
        }
        return null
    }

    private fun parseMonthIndex(name: String): Int {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.startsWith("jan") -> Calendar.JANUARY
            lower.startsWith("feb") -> Calendar.FEBRUARY
            lower.startsWith("mar") -> Calendar.MARCH
            lower.startsWith("apr") -> Calendar.APRIL
            lower.startsWith("may") -> Calendar.MAY
            lower.startsWith("jun") -> Calendar.JUNE
            lower.startsWith("jul") -> Calendar.JULY
            lower.startsWith("aug") -> Calendar.AUGUST
            lower.startsWith("sep") -> Calendar.SEPTEMBER
            lower.startsWith("oct") -> Calendar.OCTOBER
            lower.startsWith("nov") -> Calendar.NOVEMBER
            lower.startsWith("dec") -> Calendar.DECEMBER
            else -> -1
        }
    }

    private fun parseWeekday(normalized: String, cal: Calendar): String? {
        val weekdays = listOf(
            "monday" to Calendar.MONDAY,
            "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY,
            "thursday" to Calendar.THURSDAY,
            "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
            "sunday" to Calendar.SUNDAY
        )

        for ((dayName, dayConst) in weekdays) {
            val isNext = normalized.contains("next $dayName")
            if (isNext || normalized.contains(dayName)) {
                val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                var daysToAdd = (dayConst - currentDayOfWeek + 7) % 7
                if (daysToAdd == 0) daysToAdd = 7
                if (isNext && daysToAdd < 7) {
                    daysToAdd += 7
                }
                cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
                return if (isNext) "Next ${dayName.replaceFirstChar { it.uppercase() }}" else dayName.replaceFirstChar { it.uppercase() }
            }
        }
        return null
    }

    data class ParsedTimeResult(
        val startCal: Calendar,
        val endCal: Calendar,
        val startFormatted: String,
        val endFormatted: String,
        val start24: String,
        val end24: String
    )

    private fun extractTime(
        normalized: String,
        baseCal: Calendar,
        timeZone: TimeZone
    ): ParsedTimeResult {
        var startCal = baseCal.clone() as Calendar
        var endCal = baseCal.clone() as Calendar

        var explicitStartHour: Int? = null
        var explicitStartMin: Int? = null
        var explicitEndHour: Int? = null
        var explicitEndMin: Int? = null

        // 1. Check Range: "from 8 AM to 9 AM", "8 am to 9 am", "from 8:00 am to 9:00 am", "from 8 to 9 am"
        val rangeRegex = Pattern.compile(
            "(?:from\\s+)?(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)\\s+(?:to|until|-)\\s+(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm))",
            Pattern.CASE_INSENSITIVE
        )
        val rangeMatcher = rangeRegex.matcher(normalized)

        if (rangeMatcher.find()) {
            val startStr = rangeMatcher.group(1).orEmpty()
            val endStr = rangeMatcher.group(2).orEmpty()

            val endParsed = parseTimeComponents(endStr)
            var startParsed = parseTimeComponents(startStr)

            // If start lacked AM/PM (e.g. "from 8 to 9 AM"), inherit from end
            if (startParsed != null && !startStr.contains("am") && !startStr.contains("pm")) {
                if (endParsed != null && endParsed.first >= 12 && startParsed.first < 12) {
                    // inherit PM if appropriate
                    startParsed = Pair(startParsed.first + 12, startParsed.second)
                }
            }

            if (startParsed != null && endParsed != null) {
                explicitStartHour = startParsed.first
                explicitStartMin = startParsed.second
                explicitEndHour = endParsed.first
                explicitEndMin = endParsed.second
            }
        }

        // 2. If no range found, check Single Time
        if (explicitStartHour == null) {
            // Check patterns: "at 8:45 pm", "at 8 pm", "at 8:00 am", "8:45 pm", "8 am", "at 2:30 pm"
            val singleRegex = Pattern.compile(
                "(?:(?:at|around)\\s+)?(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm))",
                Pattern.CASE_INSENSITIVE
            )
            val singleMatcher = singleRegex.matcher(normalized)

            if (singleMatcher.find()) {
                val parsed = parseTimeComponents(singleMatcher.group(1).orEmpty())
                if (parsed != null) {
                    explicitStartHour = parsed.first
                    explicitStartMin = parsed.second
                }
            }
        }

        // 3. Check "at 8" without AM/PM
        if (explicitStartHour == null) {
            val bareAtRegex = Pattern.compile("(?:at|around)\\s+(\\d{1,2})(?::(\\d{2}))?\\b", Pattern.CASE_INSENSITIVE)
            val bareMatcher = bareAtRegex.matcher(normalized)
            if (bareMatcher.find()) {
                val h = bareMatcher.group(1)?.toIntOrNull() ?: 10
                val m = bareMatcher.group(2)?.toIntOrNull() ?: 0
                // Smart AM/PM determination
                val hour24 = when {
                    h in 8..11 -> h // 8 AM to 11 AM
                    h == 12 -> 12 // 12 PM
                    h in 1..7 -> h + 12 // 1 PM to 7 PM
                    else -> h
                }
                explicitStartHour = hour24
                explicitStartMin = m
            }
        }

        // Set start time (if user did NOT specify any time, use default 10:00 AM)
        val finalStartHour = explicitStartHour ?: 10
        val finalStartMin = explicitStartMin ?: 0

        startCal.set(Calendar.HOUR_OF_DAY, finalStartHour)
        startCal.set(Calendar.MINUTE, finalStartMin)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        // Set end time
        if (explicitEndHour != null && explicitEndMin != null) {
            endCal.set(Calendar.HOUR_OF_DAY, explicitEndHour)
            endCal.set(Calendar.MINUTE, explicitEndMin)
            endCal.set(Calendar.SECOND, 0)
            endCal.set(Calendar.MILLISECOND, 0)
        } else {
            // Check explicit duration
            var durationMinutes = 30
            val durMatcher = DURATION_PATTERN.matcher(normalized)
            if (durMatcher.find()) {
                val amountStr = durMatcher.group(1).orEmpty()
                val unit = durMatcher.group(2).orEmpty()
                val amount = when (amountStr) {
                    "a", "an" -> 1
                    "half", "half an" -> 30
                    else -> amountStr.toIntOrNull() ?: 30
                }
                durationMinutes = if (unit.startsWith("hour") || unit.startsWith("hr")) {
                    if (amountStr.startsWith("half")) 30 else amount * 60
                } else amount
            }
            endCal = startCal.clone() as Calendar
            endCal.add(Calendar.MINUTE, durationMinutes)
        }

        val timeFmt = SimpleDateFormat("h:mm a", Locale.US).apply { this.timeZone = timeZone }
        val startFormatted = timeFmt.format(startCal.time)
        val endFormatted = timeFmt.format(endCal.time)

        val militaryFmt = SimpleDateFormat("HH:mm", Locale.US).apply { this.timeZone = timeZone }
        val start24 = militaryFmt.format(startCal.time)
        val end24 = militaryFmt.format(endCal.time)

        return ParsedTimeResult(startCal, endCal, startFormatted, endFormatted, start24, end24)
    }

    private fun parseTimeComponents(timeStr: String): Pair<Int, Int>? {
        val trimmed = timeStr.trim().lowercase(Locale.ROOT)
        val pattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(trimmed)
        if (!matcher.find()) return null

        var hour = matcher.group(1)?.toIntOrNull() ?: return null
        val minute = matcher.group(2)?.toIntOrNull() ?: 0
        val ampm = matcher.group(3)?.lowercase(Locale.ROOT)

        if (ampm == "pm") {
            if (hour < 12) hour += 12
        } else if (ampm == "am") {
            if (hour == 12) hour = 0
        }

        return Pair(hour, minute)
    }

    private fun extractTitle(rawText: String, normalized: String, attendeeName: String?): String {
        // High priority phrase recognition
        when {
            normalized.contains("team sync") -> return "Team Sync"
            normalized.contains("team meeting") -> return "Team Meeting"
            normalized.contains("project discussion") -> return "Project Discussion"
            normalized.contains("weekly review") -> return "Weekly Review"
            normalized.contains("sprint planning") -> return "Sprint Planning"
            normalized.contains("client presentation") -> return "Client Presentation"
            normalized.contains("1:1") || normalized.contains("one on one") -> {
                return if (attendeeName != null) "1:1 with $attendeeName" else "1:1 Meeting"
            }
            normalized.contains("quick sync") -> {
                return if (attendeeName != null) "Quick Sync with $attendeeName" else "Quick Sync"
            }
        }

        var clean = rawText
        // Strip common command prefixes
        val prefixRegex = Regex("^(?:please\\s+)?(?:can you\\s+)?(?:schedule|book|set up|create)\\s+(?:a|an)?\\s*", RegexOption.IGNORE_CASE)
        clean = clean.replace(prefixRegex, "").trim()

        // If it starts with "meeting with <name>", keep that
        if (attendeeName != null && clean.lowercase(Locale.ROOT).startsWith("meeting with")) {
            return "Meeting with $attendeeName"
        }

        // Strip time clauses
        clean = clean.replace(Regex("from\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?\\s+to\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("(?:at|around)\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|a\\.m\\.|p\\.m\\.)?", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("\\b\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|a\\.m\\.|p\\.m\\.)\\b", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("\\b\\d{1,2}\\s*o'clock(?:\\s+in the (?:morning|afternoon|evening))?\\b", RegexOption.IGNORE_CASE), "")

        // Strip date clauses
        clean = clean.replace(Regex("\\b(?:day after tomorrow|tomorrow|today|tonight|yesterday)\\b", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("\\b(?:next\\s+)?(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("\\b(?:january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)\\s+\\d{1,2}(?:st|nd|rd|th)?\\b", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("\\b\\d{1,2}(?:st|nd|rd|th)?\\s+(?:of\\s+)?(?:january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)\\b", RegexOption.IGNORE_CASE), "")

        // Strip attendee and reminder clauses
        clean = clean.replace(Regex("at\\s+[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("with\\s+[A-Za-z0-9_-]+(?:\\s+[A-Za-z0-9_-]+)?", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("(?:and\\s+)?remind me.*", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("for\\s+\\d+\\s*(?:min|minute|minutes|hour|hours|mins|hrs)", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("[\\s.,;:!-]+"), " ").trim()

        return when {
            clean.isNotBlank() && clean.length > 2 -> clean.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
            clean.equals("meeting", ignoreCase = true) -> "Meeting"
            attendeeName != null -> "Meeting with $attendeeName"
            else -> "New Event"
        }
    }
}
