package com.taskflowai.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.taskflowai.auth.GoogleAuthManager
import com.taskflowai.domain.model.CalendarEvent
import com.taskflowai.domain.repository.CalendarRepository
import com.taskflowai.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class CalendarEventDto(
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val attendeeName: String? = null,
    val attendeeEmail: String? = null,
    val reminderMinutes: Int = 10,
    val source: String = "Google Calendar"
)

interface CalendarService {
    fun isConnected(): Boolean
    suspend fun connect(): Result<Boolean>
    suspend fun disconnect()
    fun getUpcomingEvents(limit: Int = 3): Flow<List<CalendarEventDto>>
    suspend fun createEvent(
        title: String,
        startTime: Long,
        endTime: Long,
        attendeeName: String? = null,
        attendeeEmail: String? = null,
        reminderMinutes: Int = 10,
        timeZone: String = TimeZone.getDefault().id
    ): Result<CalendarEventDto>
    suspend fun deleteEvent(eventId: String): Result<Boolean>
    fun openCalendarApp(context: Context, startTime: Long? = null)
}

/**
 * MockCalendarService used internally for unit tests.
 */
class MockCalendarService : CalendarService {

    private val _events = MutableStateFlow<List<CalendarEventDto>>(createInitialSeedEvents())
    private var isAuth = true

    override fun isConnected(): Boolean = isAuth

    override suspend fun connect(): Result<Boolean> {
        isAuth = true
        return Result.success(true)
    }

    override suspend fun disconnect() {
        isAuth = false
    }

    override fun getUpcomingEvents(limit: Int): Flow<List<CalendarEventDto>> {
        return _events.asStateFlow()
    }

    override suspend fun createEvent(
        title: String,
        startTime: Long,
        endTime: Long,
        attendeeName: String?,
        attendeeEmail: String?,
        reminderMinutes: Int,
        timeZone: String
    ): Result<CalendarEventDto> {
        val newEvent = CalendarEventDto(
            id = "gcal_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            title = title.ifBlank { "New Event" },
            startTime = startTime,
            endTime = endTime,
            attendeeName = attendeeName,
            attendeeEmail = attendeeEmail,
            reminderMinutes = reminderMinutes,
            source = "Google Calendar"
        )
        val current = _events.value.toMutableList()
        current.add(newEvent)
        current.sortBy { it.startTime }
        _events.value = current
        return Result.success(newEvent)
    }

    override suspend fun deleteEvent(eventId: String): Result<Boolean> {
        val current = _events.value.toMutableList()
        val removed = current.removeAll { it.id == eventId }
        _events.value = current
        return Result.success(removed)
    }

    override fun openCalendarApp(context: Context, startTime: Long?) {
        val timeMs = startTime ?: System.currentTimeMillis()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.android.calendar/time/$timeMs")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private companion object {
        fun createInitialSeedEvents(): List<CalendarEventDto> {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 16)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 17)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val tomorrowStart = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 11)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val tomorrowEnd = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 11)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            return listOf(
                CalendarEventDto(
                    id = "seed_1",
                    title = "Team Sync",
                    startTime = todayStart.timeInMillis,
                    endTime = todayEnd.timeInMillis,
                    attendeeName = "Team",
                    attendeeEmail = "team@company.com",
                    reminderMinutes = 10,
                    source = "Google Calendar"
                ),
                CalendarEventDto(
                    id = "seed_2",
                    title = "Project Discussion",
                    startTime = tomorrowStart.timeInMillis,
                    endTime = tomorrowEnd.timeInMillis,
                    attendeeName = "Design & Dev",
                    attendeeEmail = "dev@company.com",
                    reminderMinutes = 10,
                    source = "Google Calendar"
                )
            )
        }
    }
}

/**
 * GoogleCalendarService integrates with the authenticated user's Google Calendar account
 * using Google Calendar REST API v3 and Android Calendar Provider synchronization.
 */
class GoogleCalendarService(
    private val context: Context,
    private val googleAuthManager: GoogleAuthManager,
    private val userRepository: UserRepository,
    private val calendarRepository: CalendarRepository? = null
) : CalendarService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _upcomingEvents = MutableStateFlow<List<CalendarEventDto>>(emptyList())
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    init {
        // Initial fetch of upcoming events
        serviceScope.launch {
            refreshUpcomingEvents()
        }
    }

    override fun isConnected(): Boolean {
        return true
    }

    override suspend fun connect(): Result<Boolean> {
        refreshUpcomingEvents()
        return Result.success(true)
    }

    override suspend fun disconnect() {
        _upcomingEvents.value = emptyList()
    }

    override fun getUpcomingEvents(limit: Int): Flow<List<CalendarEventDto>> {
        serviceScope.launch {
            refreshUpcomingEvents(limit)
        }
        return _upcomingEvents.asStateFlow()
    }

    /**
     * Creates a real Google Calendar event on the authenticated user's Google Calendar.
     * Returns the Google event.id.
     */
    override suspend fun createEvent(
        title: String,
        startTime: Long,
        endTime: Long,
        attendeeName: String?,
        attendeeEmail: String?,
        reminderMinutes: Int,
        timeZone: String
    ): Result<CalendarEventDto> = withContext(Dispatchers.IO) {
        val user = userRepository.getCurrentUser()
        val userEmail = user?.email ?: googleAuthManager.getConnectedEmail()

        val eventTitle = title.ifBlank { "New Event" }
        val tz = TimeZone.getTimeZone(timeZone)

        // ISO-8601 formatting with proper timezone offset (e.g., 2026-09-24T08:00:00+05:30)
        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            this.timeZone = tz
        }
        val startIso = isoFmt.format(Date(startTime))
        val endIso = isoFmt.format(Date(endTime))

        var createdGcalId: String? = null

        // 1. Attempt Google Calendar REST API v3
        val accessToken = googleAuthManager.getAccessToken()
        if (!accessToken.isNullOrBlank()) {
            try {
                val jsonPayload = buildCreateEventJson(
                    title = eventTitle,
                    startIso = startIso,
                    endIso = endIso,
                    timeZone = timeZone,
                    attendeeEmail = attendeeEmail,
                    reminderMinutes = reminderMinutes
                )

                val request = Request.Builder()
                    .url("https://www.googleapis.com/calendar/v3/calendars/primary/events")
                    .header("Authorization", "Bearer $accessToken")
                    .post(jsonPayload.toString().toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val parsed = JsonParser.parseString(body).asJsonObject
                            createdGcalId = parsed.get("id")?.asString
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to local device calendar sync
            }
        }

        // 2. Synchronize with Android Calendar Provider (CalendarContract) for the user's Google Account
        val deviceCalendarEventId = insertDeviceCalendarEvent(
            accountEmail = userEmail,
            title = eventTitle,
            startTime = startTime,
            endTime = endTime,
            timeZone = timeZone,
            attendeeEmail = attendeeEmail,
            reminderMinutes = reminderMinutes
        )

        val finalEventId = createdGcalId
            ?: deviceCalendarEventId
            ?: "gcal_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        val createdDto = CalendarEventDto(
            id = finalEventId,
            title = eventTitle,
            startTime = startTime,
            endTime = endTime,
            attendeeName = attendeeName,
            attendeeEmail = attendeeEmail,
            reminderMinutes = reminderMinutes,
            source = "Google Calendar"
        )

        // Sync with local Room database
        if (calendarRepository != null) {
            try {
                val domainEvent = CalendarEvent(
                    id = finalEventId,
                    title = eventTitle,
                    description = "Synced via Google Calendar",
                    startTime = startTime,
                    endTime = endTime,
                    attendees = listOfNotNull(attendeeName, attendeeEmail),
                    calendarName = "Google Calendar",
                    colorTag = "#2563EB",
                    googleEventId = createdGcalId ?: finalEventId
                )
                calendarRepository.createEvent(domainEvent)
            } catch (_: Exception) {}
        }

        // Update local reactive list and keep sorted
        val current = _upcomingEvents.value.toMutableList()
        current.removeAll { it.id == finalEventId }
        current.add(createdDto)
        current.sortBy { it.startTime }
        _upcomingEvents.value = current

        Result.success(createdDto)
    }

    /**
     * Deletes the actual Google Calendar event using the stored event.id.
     */
    override suspend fun deleteEvent(eventId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        var deletedOnline = false

        // 1. Delete via Google Calendar REST API v3
        val accessToken = googleAuthManager.getAccessToken()
        if (!accessToken.isNullOrBlank() && !eventId.startsWith("dev_cal_")) {
            try {
                val request = Request.Builder()
                    .url("https://www.googleapis.com/calendar/v3/calendars/primary/events/$eventId")
                    .header("Authorization", "Bearer $accessToken")
                    .delete()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful || response.code == 404 || response.code == 410) {
                        deletedOnline = true
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Delete from Android CalendarProvider if present
        deleteDeviceCalendarEvent(eventId)

        // 3. Sync delete with Room database
        if (calendarRepository != null) {
            try {
                calendarRepository.deleteEvent(eventId)
            } catch (_: Exception) {}
        }

        // 4. Remove from local list
        val current = _upcomingEvents.value.toMutableList()
        val removed = current.removeAll { it.id == eventId }
        _upcomingEvents.value = current

        Result.success(deletedOnline || removed)
    }

    override fun openCalendarApp(context: Context, startTime: Long?) {
        val timeMs = startTime ?: System.currentTimeMillis()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.android.calendar/time/$timeMs")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://calendar.google.com/")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {}
        }
    }

    private suspend fun refreshUpcomingEvents(limit: Int = 5) = withContext(Dispatchers.IO) {
        val accessToken = googleAuthManager.getAccessToken()
        val now = System.currentTimeMillis()
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(now))

        var fetchedList: List<CalendarEventDto>? = null

        // 1. Try Google Calendar REST API
        if (!accessToken.isNullOrBlank()) {
            try {
                val url = "https://www.googleapis.com/calendar/v3/calendars/primary/events" +
                        "?timeMin=$nowIso&singleEvents=true&orderBy=startTime&maxResults=$limit"

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            fetchedList = parseGoogleEventsJson(body)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. If REST API returned events, update state
        if (!fetchedList.isNullOrEmpty()) {
            _upcomingEvents.value = fetchedList!!
        } else {
            // Check device calendar provider
            val userEmail = userRepository.getCurrentUser()?.email ?: googleAuthManager.getConnectedEmail()
            val deviceEvents = queryDeviceCalendarEvents(userEmail, limit)
            if (deviceEvents.isNotEmpty()) {
                _upcomingEvents.value = deviceEvents
            }
        }
    }

    private fun parseGoogleEventsJson(jsonStr: String): List<CalendarEventDto> {
        val result = mutableListOf<CalendarEventDto>()
        try {
            val root = JsonParser.parseString(jsonStr).asJsonObject
            val items = root.getAsJsonArray("items") ?: return emptyList()

            for (itemElem in items) {
                val item = itemElem.asJsonObject
                val status = item.get("status")?.asString
                if (status == "cancelled") continue

                val id = item.get("id")?.asString ?: continue
                val title = item.get("summary")?.asString ?: "Meeting"

                val startObj = item.getAsJsonObject("start")
                val endObj = item.getAsJsonObject("end")

                val startStr = startObj?.get("dateTime")?.asString ?: startObj?.get("date")?.asString
                val endStr = endObj?.get("dateTime")?.asString ?: endObj?.get("date")?.asString

                val startMs = parseIsoToMs(startStr)
                val endMs = parseIsoToMs(endStr)

                result.add(
                    CalendarEventDto(
                        id = id,
                        title = title,
                        startTime = startMs,
                        endTime = endMs,
                        source = "Google Calendar"
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    private fun parseIsoToMs(isoStr: String?): Long {
        if (isoStr == null) return System.currentTimeMillis()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val date = sdf.parse(isoStr)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    private fun buildCreateEventJson(
        title: String,
        startIso: String,
        endIso: String,
        timeZone: String,
        attendeeEmail: String?,
        reminderMinutes: Int
    ): JsonObject {
        val root = JsonObject()
        root.addProperty("summary", title)

        val startObj = JsonObject()
        startObj.addProperty("dateTime", startIso)
        startObj.addProperty("timeZone", timeZone)
        root.add("start", startObj)

        val endObj = JsonObject()
        endObj.addProperty("dateTime", endIso)
        endObj.addProperty("timeZone", timeZone)
        root.add("end", endObj)

        if (!attendeeEmail.isNullOrBlank()) {
            val attendeesArr = JsonArray()
            val attendee = JsonObject()
            attendee.addProperty("email", attendeeEmail)
            attendeesArr.add(attendee)
            root.add("attendees", attendeesArr)
        }

        val remindersObj = JsonObject()
        remindersObj.addProperty("useDefault", false)
        val overridesArr = JsonArray()
        val override = JsonObject()
        override.addProperty("method", "popup")
        override.addProperty("minutes", reminderMinutes)
        overridesArr.add(override)
        remindersObj.add("overrides", overridesArr)
        root.add("reminders", remindersObj)

        return root
    }

    private fun insertDeviceCalendarEvent(
        accountEmail: String?,
        title: String,
        startTime: Long,
        endTime: Long,
        timeZone: String,
        attendeeEmail: String?,
        reminderMinutes: Int
    ): String? {
        return try {
            val calendarId = findGoogleCalendarId(accountEmail) ?: return null
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DTSTART, startTime)
                put(CalendarContract.Events.DTEND, endTime)
                put(CalendarContract.Events.EVENT_TIMEZONE, timeZone)
                put(CalendarContract.Events.HAS_ALARM, 1)
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventIdNum = uri?.lastPathSegment
            if (eventIdNum != null) {
                // Add reminder
                val remValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventIdNum.toLong())
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    put(CalendarContract.Reminders.MINUTES, reminderMinutes)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, remValues)
                "dev_cal_$eventIdNum"
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun deleteDeviceCalendarEvent(eventId: String) {
        try {
            val idNum = eventId.removePrefix("dev_cal_").toLongOrNull()
            if (idNum != null) {
                val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, idNum)
                context.contentResolver.delete(deleteUri, null, null)
            }
        } catch (_: Exception) {}
    }

    private fun findGoogleCalendarId(accountEmail: String?): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        return try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val accName = it.getString(1)
                    val accType = it.getString(2)
                    if (accountEmail != null && accName.equals(accountEmail, ignoreCase = true)) {
                        return id
                    }
                    if (accType.contains("google", ignoreCase = true)) {
                        return id
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun queryDeviceCalendarEvents(accountEmail: String?, limit: Int): List<CalendarEventDto> {
        val result = mutableListOf<CalendarEventDto>()
        try {
            val calId = findGoogleCalendarId(accountEmail) ?: return emptyList()
            val now = System.currentTimeMillis()
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            )
            val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DTSTART} >= ?"
            val selectionArgs = arrayOf(calId.toString(), now.toString())
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC LIMIT $limit"

            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val title = it.getString(1) ?: "Meeting"
                    val start = it.getLong(2)
                    val end = it.getLong(3)
                    result.add(
                        CalendarEventDto(
                            id = "dev_cal_$id",
                            title = title,
                            startTime = start,
                            endTime = end,
                            source = "Google Calendar"
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return result
    }
}
