package com.taskflowai

import com.taskflowai.calendar.ScheduleCommandParser
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class ScheduleCommandParserTest {

    private val testTimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    // =========================================================================
    // MANDATORY TEST CASES (Requirement 34)
    // =========================================================================

    @Test
    fun `TEST 1 - Schedule a meeting tomorrow at 8 AM`() {
        val input = "Schedule a meeting tomorrow at 8 AM."
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("Tomorrow", parsed.dateLabel)
        assertEquals("8:00 AM", parsed.startFormatted)
        assertEquals("8:30 AM", parsed.endFormatted)
        assertEquals("08:00", parsed.startTime24)
        assertEquals("08:30", parsed.endTime24)
        assertTrue(parsed.title.contains("Meeting"))
    }

    @Test
    fun `TEST 2 - Schedule a meeting tomorrow at 8 PM`() {
        val input = "Schedule a meeting tomorrow at 8 PM."
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("Tomorrow", parsed.dateLabel)
        assertEquals("8:00 PM", parsed.startFormatted)
        assertEquals("8:30 PM", parsed.endFormatted)
        assertEquals("20:00", parsed.startTime24)
        assertEquals("20:30", parsed.endTime24)
    }

    @Test
    fun `TEST 3 - Schedule a meeting tomorrow at 10 AM`() {
        val input = "Schedule a meeting tomorrow at 10 AM."
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("Tomorrow", parsed.dateLabel)
        assertEquals("10:00 AM", parsed.startFormatted)
        assertEquals("10:30 AM", parsed.endFormatted)
        assertEquals("10:00", parsed.startTime24)
        assertEquals("10:30", parsed.endTime24)
    }

    @Test
    fun `TEST 4 - Schedule a meeting tomorrow at 2 30 PM`() {
        val input = "Schedule a meeting tomorrow at 2:30 PM."
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("Tomorrow", parsed.dateLabel)
        assertEquals("2:30 PM", parsed.startFormatted)
        assertEquals("3:00 PM", parsed.endFormatted)
        assertEquals("14:30", parsed.startTime24)
        assertEquals("15:00", parsed.endTime24)
    }

    @Test
    fun `TEST 5 - Schedule a meeting tomorrow from 8 AM to 9 AM`() {
        val input = "Schedule a meeting tomorrow from 8 AM to 9 AM."
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("Tomorrow", parsed.dateLabel)
        assertEquals("8:00 AM", parsed.startFormatted)
        assertEquals("9:00 AM", parsed.endFormatted)
        assertEquals("08:00", parsed.startTime24)
        assertEquals("09:00", parsed.endTime24)
    }

    @Test
    fun `TEST 6 - Schedule a meeting tomorrow at 8 45 PM`() {
        val input = "Schedule a meeting tomorrow at 8:45 PM."
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("Tomorrow", parsed.dateLabel)
        assertEquals("8:45 PM", parsed.startFormatted)
        assertEquals("9:15 PM", parsed.endFormatted)
        assertEquals("20:45", parsed.startTime24)
        assertEquals("21:15", parsed.endTime24)
    }

    // =========================================================================
    // NATURAL SPEECH VARIATION TESTS
    // =========================================================================

    @Test
    fun `test speech with dots in am - 8 a_m_`() {
        val input = "Schedule a meeting tomorrow at 8 a.m."
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("8:00 AM", parsed.startFormatted)
        assertEquals("8:30 AM", parsed.endFormatted)
    }

    @Test
    fun `test word numbers - eight am`() {
        val input = "Schedule a meeting tomorrow at eight am"
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("8:00 AM", parsed.startFormatted)
        assertEquals("8:30 AM", parsed.endFormatted)
    }

    @Test
    fun `test word numbers - eight thirty am`() {
        val input = "Schedule a meeting tomorrow at eight thirty am"
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("8:30 AM", parsed.startFormatted)
        assertEquals("9:00 AM", parsed.endFormatted)
    }

    @Test
    fun `test natural phrase - 8 o'clock in the morning`() {
        val input = "Schedule a meeting tomorrow at 8 o'clock in the morning"
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("8:00 AM", parsed.startFormatted)
        assertEquals("8:30 AM", parsed.endFormatted)
    }

    @Test
    fun `test day after tomorrow`() {
        val input = "Schedule a team meeting day after tomorrow at 3 PM"
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("Day after tomorrow", parsed.dateLabel)
        assertEquals("3:00 PM", parsed.startFormatted)
        assertEquals("3:30 PM", parsed.endFormatted)
    }

    @Test
    fun `test full command with time range, attendee, and email`() {
        val input = "Schedule a team sync tomorrow from 10 AM to 11 AM with Alex at alex@gmail.com"
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("Team Sync", parsed.title)
        assertEquals("Tomorrow", parsed.dateLabel)
        assertEquals("10:00 AM", parsed.startFormatted)
        assertEquals("11:00 AM", parsed.endFormatted)
        assertEquals("Alex", parsed.attendeeName)
        assertEquals("alex@gmail.com", parsed.attendeeEmail)
        assertEquals(10, parsed.reminderMinutes)
        assertEquals("10 minutes before", parsed.reminderLabel)
    }

    @Test
    fun `test schedule 1 on 1 with Sarah`() {
        val input = "Schedule a 1:1 with Sarah tomorrow at 3 PM"
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertTrue(parsed.title.contains("1:1"))
        assertEquals("Sarah", parsed.attendeeName)
        assertEquals("Tomorrow", parsed.dateLabel)
        assertEquals("3:00 PM", parsed.startFormatted)
        assertEquals("3:30 PM", parsed.endFormatted)
    }

    @Test
    fun `test fallback default title when none specified`() {
        val input = "Tomorrow at 5 PM"
        val parsed = ScheduleCommandParser.parse(input, testTimeZone)

        assertEquals("New Event", parsed.title)
        assertEquals("Tomorrow", parsed.dateLabel)
        assertEquals("5:00 PM", parsed.startFormatted)
        assertEquals("5:30 PM", parsed.endFormatted)
    }
}
