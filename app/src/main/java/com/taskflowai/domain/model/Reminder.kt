package com.taskflowai.domain.model

enum class ReminderTiming(val minutesBefore: Int, val label: String) {
    MINUTES_10(10, "10 minutes before"),
    MINUTES_15(15, "15 minutes before"),
    MINUTES_30(30, "30 minutes before"),
    HOURS_1(60, "1 hour before"),
    DAYS_1(1440, "1 day before"),
    CUSTOM(-1, "Custom timing")
}

enum class ReminderStatus {
    SCHEDULED,
    TRIGGERED,
    CANCELLED,
    DISMISSED
}

data class Reminder(
    val id: String,
    val taskId: String? = null,
    val calendarEventId: String? = null,
    val title: String,
    val description: String = "",
    val triggerTime: Long,
    val timing: ReminderTiming = ReminderTiming.MINUTES_30,
    val customMinutesBefore: Int? = null,
    val status: ReminderStatus = ReminderStatus.SCHEDULED,
    val createdAt: Long = System.currentTimeMillis()
)
