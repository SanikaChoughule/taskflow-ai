package com.taskflowai.data.repository

import com.taskflowai.data.database.dao.ScheduleHistoryDao
import com.taskflowai.data.database.dao.TaskDao
import com.taskflowai.data.database.dao.UserDao
import com.taskflowai.data.database.entity.ScheduleHistoryEntity
import com.taskflowai.data.database.entity.TaskEntity
import com.taskflowai.data.mapper.DataMappers.toDomain
import com.taskflowai.data.mapper.DataMappers.toEntity
import com.taskflowai.data.remote.api.TaskFlowApi
import com.taskflowai.data.remote.dto.UserSyncDto
import com.taskflowai.domain.model.AuthState
import com.taskflowai.domain.model.User
import com.taskflowai.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val taskDao: TaskDao? = null,
    private val scheduleHistoryDao: ScheduleHistoryDao? = null,
    private val api: TaskFlowApi? = null
) : UserRepository {

    override fun getAuthState(): Flow<AuthState> {
        return userDao.getUserFlow().map { userEntity ->
            if (userEntity != null) {
                AuthState.Authenticated(userEntity.toDomain())
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    override suspend fun getCurrentUser(): User? {
        return userDao.getUser()?.toDomain()
    }

    override suspend fun saveUser(user: User) {
        userDao.insertUser(user.toEntity())
        // Preserve all existing pre-login or unassigned database records by attaching them to the user
        taskDao?.associateUnassignedTasksToUser(user.id)
        scheduleHistoryDao?.associateUnassignedHistoryToUser(user.id)

        // Sync user to remote MySQL database API
        if (api != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    api.syncUser(
                        UserSyncDto(
                            id = user.id,
                            email = user.email,
                            name = user.name,
                            photoUrl = user.photoUrl,
                            isGoogleConnected = user.isGoogleConnected,
                            isCalendarConnected = user.isCalendarConnected,
                            lastLoginAt = user.lastLoginAt
                        )
                    )
                }

                // Fetch remote user tasks from MySQL database and save into Room database
                runCatching {
                    val tasksResp = api.fetchUserTasks(user.id)
                    if (tasksResp.isSuccessful) {
                        tasksResp.body()?.forEach { remoteTask ->
                            taskDao?.insertTask(
                                TaskEntity(
                                    id = remoteTask.id,
                                    userId = remoteTask.userId,
                                    title = remoteTask.title,
                                    description = remoteTask.description,
                                    originalCommand = remoteTask.originalCommand,
                                    status = remoteTask.status,
                                    priority = remoteTask.priority,
                                    createdAt = remoteTask.createdAt,
                                    completedAt = remoteTask.completedAt,
                                    linkedCalendarEventId = remoteTask.linkedCalendarEventId,
                                    linkedReminderId = remoteTask.linkedReminderId,
                                    isUndoable = remoteTask.isUndoable,
                                    errorMessage = remoteTask.errorMessage
                                )
                            )
                        }
                    }
                }

                // Fetch remote user history from MySQL database and save into Room database
                runCatching {
                    val historyResp = api.fetchUserHistory(user.id)
                    if (historyResp.isSuccessful) {
                        historyResp.body()?.forEach { remoteHistory ->
                            scheduleHistoryDao?.insertHistory(
                                ScheduleHistoryEntity(
                                    id = remoteHistory.id,
                                    userId = remoteHistory.userId,
                                    title = remoteHistory.title,
                                    scheduledDate = remoteHistory.scheduledDate,
                                    startTimeFormatted = remoteHistory.startTimeFormatted,
                                    endTimeFormatted = remoteHistory.endTimeFormatted,
                                    startTimeMs = remoteHistory.startTimeMs,
                                    endTimeMs = remoteHistory.endTimeMs,
                                    attendee = remoteHistory.attendee,
                                    attendeeEmail = remoteHistory.attendeeEmail,
                                    action = remoteHistory.action,
                                    actionTime = remoteHistory.actionTime,
                                    calendarEventId = remoteHistory.calendarEventId
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun updateCalendarConnected(isConnected: Boolean) {
        val user = userDao.getUser()
        if (user != null) {
            userDao.updateCalendarConnected(user.id, isConnected)
        }
    }

    override suspend fun signOut() {
        userDao.clearUser()
    }
}
