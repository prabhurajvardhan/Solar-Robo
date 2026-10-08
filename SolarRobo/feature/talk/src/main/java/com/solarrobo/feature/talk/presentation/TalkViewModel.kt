package com.solarrobo.feature.talk.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repository: TalkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TalkUiState())
    val uiState: StateFlow<TalkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getMessageHistory().collect { history ->
                _uiState.update { it.copy(messages = history) }
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
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