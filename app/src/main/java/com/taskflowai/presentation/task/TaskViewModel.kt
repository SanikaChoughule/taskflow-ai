package com.taskflowai.presentation.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.domain.model.*
import com.taskflowai.domain.repository.TaskRepository
import com.taskflowai.domain.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TaskPlanUiState {
    data object Idle : TaskPlanUiState()
    data object Planning : TaskPlanUiState()
    data class Ready(val command: AICommand, val plan: TaskPlan, val conflict: Conflict?) : TaskPlanUiState()
    data class Error(val message: String) : TaskPlanUiState()
}

sealed class ExecutionUiState {
    data object Idle : ExecutionUiState()
    data class InProgress(
        val taskId: String,
        val subTasks: List<SubTask>,
        val currentStepIndex: Int,
        val currentStepTitle: String
    ) : ExecutionUiState()
    data class Completed(val result: TaskExecutionResult) : ExecutionUiState()
    data class Failed(val taskId: String, val error: String) : ExecutionUiState()
}

class TaskViewModel(
    private val processCommandUseCase: ProcessCommandUseCase,
    private val executeTaskPlanUseCase: ExecuteTaskPlanUseCase,
    private val undoActionUseCase: UndoActionUseCase,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _planState = MutableStateFlow<TaskPlanUiState>(TaskPlanUiState.Idle)
    val planState: StateFlow<TaskPlanUiState> = _planState.asStateFlow()

    private val _executionState = MutableStateFlow<ExecutionUiState>(ExecutionUiState.Idle)
    val executionState: StateFlow<ExecutionUiState> = _executionState.asStateFlow()

    private val _taskDetails = MutableStateFlow<Task?>(null)
    val taskDetails: StateFlow<Task?> = _taskDetails.asStateFlow()

    fun loadPlanForCommand(rawCommand: String) {
        viewModelScope.launch {
            _planState.value = TaskPlanUiState.Planning
            when (val result = processCommandUseCase(rawCommand)) {
                is CommandValidationResult.Valid -> {
                    _planState.value = TaskPlanUiState.Ready(
                        command = result.command,
                        plan = result.plan,
                        conflict = result.conflict
                    )
                }
                is CommandValidationResult.Ambiguous -> {
                    _planState.value = TaskPlanUiState.Error(result.message)
                }
                is CommandValidationResult.Error -> {
                    _planState.value = TaskPlanUiState.Error(result.error)
                }
            }
        }
    }

    fun executePlan(bypassConflict: Boolean = false) {
        val currentPlan = (_planState.value as? TaskPlanUiState.Ready) ?: return

        viewModelScope.launch {
            executeTaskPlanUseCase.execute(
                command = currentPlan.command,
                plan = currentPlan.plan,
                bypassConflict = bypassConflict
            ).collect { progress ->
                when (progress) {
                    is ExecutionProgress.StepUpdate -> {
                        _executionState.value = ExecutionUiState.InProgress(
                            taskId = progress.taskId,
                            subTasks = progress.subTasks,
                            currentStepIndex = progress.currentStepIndex,
                            currentStepTitle = progress.currentStepTitle
                        )
                    }
                    is ExecutionProgress.Completed -> {
                        _executionState.value = ExecutionUiState.Completed(progress.result)
                    }
                    is ExecutionProgress.Failed -> {
                        _executionState.value = ExecutionUiState.Failed(progress.taskId, progress.error)
                    }
                }
            }
        }
    }

    fun triggerUndo(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = undoActionUseCase()) {
                is UndoResult.Success -> onComplete(result.message)
                is UndoResult.Failure -> onComplete(result.error)
                UndoResult.NothingToUndo -> onComplete("Nothing to undo.")
            }
        }
    }

    fun loadTaskDetails(taskId: String) {
        viewModelScope.launch {
            _taskDetails.value = taskRepository.getTaskById(taskId)
        }
    }
}
