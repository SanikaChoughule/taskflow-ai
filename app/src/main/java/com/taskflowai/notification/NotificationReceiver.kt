package com.taskflowai.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver {
    constructor() : super()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getStringExtra("reminder_id") ?: return

        when (action) {
            "ACTION_DISMISS_REMINDER" -> {
                // Handled via notification dismiss
            }
            "ACTION_SNOOZE_REMINDER" -> {
                // Future snooze functionality
            }
        }
    }
}
