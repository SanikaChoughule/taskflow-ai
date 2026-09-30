package com.taskflowai.app

import android.content.Context
import com.taskflowai.BuildConfig
import com.taskflowai.ai.AIServiceFactory
import com.taskflowai.auth.GoogleAuthManager
import com.taskflowai.calendar.GoogleCalendarService
import com.taskflowai.data.database.TaskFlowDatabase
import com.taskflowai.data.remote.api.TaskFlowApi
import com.taskflowai.data.remote.interceptor.AuthInterceptor
import com.taskflowai.data.repository.*
import com.taskflowai.domain.repository.*
import com.taskflowai.domain.usecase.*
import com.taskflowai.notification.NotificationManagerHelper
import com.taskflowai.security.SecureTokenManager
import com.taskflowai.voice.VoiceRecognizerManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(val context: Context) {

    // Database
    val database: TaskFlowDatabase by lazy {
        TaskFlowDatabase.getInstance(context)
    }

    // Security & Auth
    val secureTokenManager: SecureTokenManager by lazy {
        SecureTokenManager(context)
    }

    // Network & API
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(secureTokenManager))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val taskFlowApi: TaskFlowApi? by lazy {
        val baseUrl = BuildConfig.BACKEND_BASE_URL
        if (baseUrl.isNotBlank() && !baseUrl.contains("mock")) {
            runCatching {
                Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(TaskFlowApi::class.java)
            }.getOrNull()
        } else null
    }

    // AI
    val aiServiceFactory: AIServiceFactory by lazy {
        AIServiceFactory()
    }

    // Repositories
    val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(database.taskDao(), database.subTaskDao(), taskFlowApi)
    }

    val calendarRepository: CalendarRepository by lazy {
        CalendarRepositoryImpl(database.calendarEventDao())
    }

    val actionLogRepository: ActionLogRepository by lazy {
        ActionLogRepositoryImpl(database.actionLogDao())
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryImpl(database.reminderDao())
    }

    val suggestionRepository: SuggestionRepository by lazy {
        SuggestionRepositoryImpl(database.suggestionDao(), aiServiceFactory.getService("LOCAL"))
    }

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(database.userDao(), database.taskDao(), database.scheduleHistoryDao(), taskFlowApi)
    }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepositoryImpl(context)
    }

    val undoRepository: UndoRepository by lazy {
        UndoRepositoryImpl()
    }

    val mockCalendarService: com.taskflowai.calendar.MockCalendarService by lazy {
        com.taskflowai.calendar.MockCalendarService()
    }

    val googleCalendarService: com.taskflowai.calendar.GoogleCalendarService by lazy {
        GoogleCalendarService(context, googleAuthManager, userRepository, calendarRepository)
    }

    val calendarService: com.taskflowai.calendar.CalendarService by lazy {
        googleCalendarService
    }

    val scheduleHistoryRepository: ScheduleHistoryRepository by lazy {
        com.taskflowai.data.repository.ScheduleHistoryRepositoryImpl(database.scheduleHistoryDao(), taskFlowApi)
    }

    val googleAuthManager: GoogleAuthManager by lazy {
        GoogleAuthManager(context, userRepository, secureTokenManager)
    }

    val notificationHelper: NotificationManagerHelper by lazy {
        NotificationManagerHelper(context)
    }

    val voiceRecognizerManager: VoiceRecognizerManager by lazy {
        VoiceRecognizerManager(context)
    }

    // Use Cases
    val processCommandUseCase: ProcessCommandUseCase by lazy {
        ProcessCommandUseCase(aiServiceFactory.getService("LOCAL"), calendarRepository)
    }

    val executeTaskPlanUseCase: ExecuteTaskPlanUseCase by lazy {
        ExecuteTaskPlanUseCase(
            taskRepository = taskRepository,
            calendarRepository = calendarRepository,
            reminderRepository = reminderRepository,
            actionLogRepository = actionLogRepository,
            undoRepository = undoRepository,
            userRepository = userRepository
        )
    }

    val undoActionUseCase: UndoActionUseCase by lazy {
        UndoActionUseCase(
            undoRepository = undoRepository,
            calendarRepository = calendarRepository,
            reminderRepository = reminderRepository,
            taskRepository = taskRepository,
            actionLogRepository = actionLogRepository
        )
    }

    val detectCalendarConflictsUseCase: DetectCalendarConflictsUseCase by lazy {
        DetectCalendarConflictsUseCase(calendarRepository)
    }

    val manageRemindersUseCase: ManageRemindersUseCase by lazy {
        ManageRemindersUseCase(reminderRepository)
    }

    val getDashboardDataUseCase: GetDashboardDataUseCase by lazy {
        GetDashboardDataUseCase(
            userRepository = userRepository,
            taskRepository = taskRepository,
            calendarRepository = calendarRepository,
            reminderRepository = reminderRepository,
            suggestionRepository = suggestionRepository,
            actionLogRepository = actionLogRepository,
            undoRepository = undoRepository
        )
    }
}
