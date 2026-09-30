package com.taskflowai.domain.repository

import com.taskflowai.domain.model.UndoAction
import kotlinx.coroutines.flow.Flow

interface UndoRepository {
    fun getLatestUndoAction(): Flow<UndoAction?>
    suspend fun pushUndoAction(action: UndoAction)
    suspend fun popUndoAction(): UndoAction?
    suspend fun clearUndoActions()
}
