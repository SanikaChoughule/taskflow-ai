package com.taskflowai.data.repository

import com.taskflowai.domain.model.UndoAction
import com.taskflowai.domain.repository.UndoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

class UndoRepositoryImpl : UndoRepository {
    private val undoStack = ArrayDeque<UndoAction>()
    private val _latestUndo = MutableStateFlow<UndoAction?>(null)

    override fun getLatestUndoAction(): Flow<UndoAction?> = _latestUndo.asStateFlow()

    override suspend fun pushUndoAction(action: UndoAction) {
        undoStack.push(action)
        _latestUndo.value = action
    }

    override suspend fun popUndoAction(): UndoAction? {
        if (undoStack.isEmpty()) return null
        val popped = undoStack.pop()
        _latestUndo.value = undoStack.peek()
        return popped
    }

    override suspend fun clearUndoActions() {
        undoStack.clear()
        _latestUndo.value = null
    }
}
