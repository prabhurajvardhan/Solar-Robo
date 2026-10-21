package com.solarrobo.feature.talk.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.core.ai.ModelInstallState
import com.solarrobo.core.ai.TernaryModelRepository
import com.solarrobo.feature.talk.domain.TalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TalkViewModel @Inject constructor(
    private val repository: TalkRepository,
    private val modelRepository: TernaryModelRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TalkUiState())
    val uiState: StateFlow<TalkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getMessageHistory().collect { history ->
                _uiState.update { it.copy(messages = history) }
            }
        }
        viewModelScope.launch {
            modelRepository?.statuses?.collect { statuses ->
                val installed = statuses
                    .filter { it.state == ModelInstallState.INSTALLED || it.state == ModelInstallState.ACTIVE }
                    .map { it.model }
                val selected = statuses.firstOrNull { it.state == ModelInstallState.ACTIVE }?.model?.id
                _uiState.update { it.copy(installedModels = installed, selectedModelId = selected) }
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun selectModel(modelId: String) {
        _uiState.value.installedModels.firstOrNull { it.id == modelId }?.let { modelRepository?.activate(it) }
    }

    fun sendMessage() {
        val message = _uiState.value.inputText
        if (message.isBlank() || _uiState.value.isSending) return

        _uiState.update {
            it.copy(inputText = "", isSending = true, error = null, lastIntent = null, lastConfidence = null)
        }
        viewModelScope.launch {
            try {
                val response = repository.sendMessage(message.trim())
                _uiState.update {
                    it.copy(
                        isSending = false,
                        lastIntent = response.intent,
                        lastConfidence = response.confidence
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = exception.localizedMessage
                            ?.takeIf { it.isNotBlank() }
                            ?: "Unable to get a response right now."
                    )
                }
            }
        }
    }
}
