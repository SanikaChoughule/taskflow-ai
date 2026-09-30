package com.taskflowai.presentation.command

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflowai.domain.usecase.CommandValidationResult
import com.taskflowai.domain.usecase.ProcessCommandUseCase
import com.taskflowai.voice.VoiceRecognizerManager
import com.taskflowai.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CommandUiState {
    data object Idle : CommandUiState()
    data object Analyzing : CommandUiState()
    data class ReadyToPlan(val validationResult: CommandValidationResult.Valid) : CommandUiState()
    data class Ambiguous(val message: String) : CommandUiState()
    data class Error(val message: String) : CommandUiState()
}

class CommandViewModel(
    private val voiceRecognizerManager: VoiceRecognizerManager,
    private val processCommandUseCase: ProcessCommandUseCase
) : ViewModel() {

    val voiceState: StateFlow<VoiceState> = voiceRecognizerManager.voiceState

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _uiState = MutableStateFlow<CommandUiState>(CommandUiState.Idle)
    val uiState: StateFlow<CommandUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            voiceState.collect { state ->
                if (state is VoiceState.Result) {
                    _inputText.value = state.recognizedText
                }
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _inputText.value = newText
        if (_uiState.value !is CommandUiState.Idle) {
            _uiState.value = CommandUiState.Idle
        }
    }

    fun startVoiceRecording() {
        voiceRecognizerManager.startListening()
    }

    fun stopVoiceRecording() {
        voiceRecognizerManager.stopListening()
    }

    fun analyzeAndPlan(onSuccess: (CommandValidationResult.Valid) -> Unit) {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = CommandUiState.Analyzing
            when (val result = processCommandUseCase(text)) {
                is CommandValidationResult.Valid -> {
                    _uiState.value = CommandUiState.ReadyToPlan(result)
                    onSuccess(result)
                }
                is CommandValidationResult.Ambiguous -> {
                    _uiState.value = CommandUiState.Ambiguous(result.message)
                }
                is CommandValidationResult.Error -> {
                    _uiState.value = CommandUiState.Error(result.error)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceRecognizerManager.reset()
    }
}
