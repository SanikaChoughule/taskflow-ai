package com.taskflowai.data.remote.api

import com.taskflowai.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface TaskFlowApi {
    @POST("tasks/understand")
    suspend fun analyzeCommand(@Body request: CommandRequestDto): Response<TaskPlanResponseDto>

    @GET("calendar/events")
    suspend fun fetchCalendarEvents(): Response<List<CalendarSyncEventDto>>

    // -------------------------------------------------------------------------
    // MySQL Multi-User Sync Endpoints
    // -------------------------------------------------------------------------
    @POST("auth/sync")
    suspend fun syncUser(@Body user: UserSyncDto): Response<ApiResponseDto>

    @GET("tasks")
    suspend fun fetchUserTasks(@Query("userId") userId: String): Response<List<TaskSyncDto>>

    @POST("tasks")
    suspend fun saveUserTask(@Body task: TaskSyncDto): Response<ApiResponseDto>

    @GET("history")
    suspend fun fetchUserHistory(@Query("userId") userId: String): Response<List<HistorySyncDto>>

    @POST("history")
    suspend fun saveUserHistory(@Body history: HistorySyncDto): Response<ApiResponseDto>

    @POST("history/undo")
    suspend fun undoUserHistory(@Body undo: UndoSyncDto): Response<ApiResponseDto>

    @DELETE("history")
    suspend fun clearUserHistory(@Query("userId") userId: String): Response<ApiResponseDto>
}
