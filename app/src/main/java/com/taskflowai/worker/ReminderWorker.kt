package com.taskflowai.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskflowai.data.database.TaskFlowDatabase
import com.taskflowai.domain.model.ReminderStatus
import com.taskflowai.notification.NotificationManagerHelper

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString("reminder_id") ?: return Result.failure()
        val title = inputData.getString("title") ?: "Reminder"
        val message = inputData.getString("message") ?: "You have an upcoming task."

        val db = TaskFlowDatabase.getInstance(applicationContext)
        val reminder = db.reminderDao().getReminderById(reminderId)

        if (reminder != null && reminder.status == ReminderStatus.SCHEDULED.name) {
            val notificationHelper = NotificationManagerHelper(applicationContext)
            notificationHelper.showReminderNotification(
                notificationId = reminderId.hashCode(),
                title = title,
                content = message
            )

            db.reminderDao().updateReminderStatus(reminderId, ReminderStatus.TRIGGERED.name)
        }

        return Result.success()
    }
}
