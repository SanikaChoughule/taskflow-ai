package com.taskflowai.data.repository

import com.taskflowai.data.database.dao.SubTaskDao
import com.taskflowai.data.database.dao.TaskDao
import com.taskflowai.data.mapper.DataMappers.toDomain
import com.taskflowai.data.mapper.DataMappers.toEntity
import com.taskflowai.data.remote.api.TaskFlowApi
import com.taskflowai.data.remote.dto.TaskSyncDto
import com.taskflowai.domain.model.SubTask
import com.taskflowai.domain.model.Task
import com.taskflowai.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val subTaskDao: SubTaskDao,
    private val api: TaskFlowApi? = null
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { list -> list.map { it.toDomain() } }
    }

    override fun getRecentTasks(limit: Int): Flow<List<Task>> {
        return taskDao.getRecentTasks(limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)?.toDomain()
    }

    override suspend fun insertTask(task: Task) {
        taskDao.insertTask(task.toEntity())

        if (api != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    api.saveUserTask(
                        TaskSyncDto(
                            id = task.id,
                            userId = task.userId,
                            title = task.title,
                            description = task.description,
                            originalCommand = task.originalCommand,
                            status = task.status.name,
                            priority = task.priority.name,
                            createdAt = task.createdAt,
                            completedAt = task.completedAt,
                            linkedCalendarEventId = task.linkedCalendarEventId,
                            linkedReminderId = task.linkedReminderId,
                            isUndoable = task.isUndoable,
                            errorMessage = task.errorMessage
                        )
                    )
                }
            }
        }
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())

        if (api != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    api.saveUserTask(
                        TaskSyncDto(
                            id = task.id,
                            userId = task.userId,
                            title = task.title,
                            description = task.description,
                            originalCommand = task.originalCommand,
                            status = task.status.name,
                            priority = task.priority.name,
                            createdAt = task.createdAt,
                            completedAt = task.completedAt,
                            linkedCalendarEventId = task.linkedCalendarEventId,
                            linkedReminderId = task.linkedReminderId,
                            isUndoable = task.isUndoable,
                            errorMessage = task.errorMessage
                        )
                    )
                }
            }
        }
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteTask(taskId)
    }

    override fun getSubTasksForTask(taskId: String): Flow<List<SubTask>> {
        return subTaskDao.getSubTasksForTask(taskId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertSubTasks(subTasks: List<SubTask>) {
        subTaskDao.insertSubTasks(subTasks.map { it.toEntity() })
    }

    override suspend fun updateSubTask(subTask: SubTask) {
        subTaskDao.updateSubTask(subTask.toEntity())
    }
}
