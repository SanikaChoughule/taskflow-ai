package com.taskflowai.domain.repository

import com.taskflowai.domain.model.SubTask
import com.taskflowai.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getRecentTasks(limit: Int = 5): Flow<List<Task>>
    suspend fun getTaskById(taskId: String): Task?
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: String)

    fun getSubTasksForTask(taskId: String): Flow<List<SubTask>>
    suspend fun insertSubTasks(subTasks: List<SubTask>)
    suspend fun updateSubTask(subTask: SubTask)
}
