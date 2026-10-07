package com.solarrobo.feature.safety.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.safety.domain.SafetyContext
import com.solarrobo.feature.safety.domain.SafetyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafetyViewModel @Inject constructor(
    private val repository: SafetyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SafetyUiState())
    val uiState: StateFlow<SafetyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSafetyEvents()
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.localizedMessage
                                ?: "Unable to load safety incidents."
                        )
                    }
                }
                .collect { incidents ->
                    val activeIncidents = incidents.filterNot { it.acknowledged }
                    val level = activeIncidents.maxByOrNull { it.level.ordinal }?.level
                        ?: SafetyLevel.NORMAL
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentLevel = level,
                            incidents = incidents
                        )
                    }
                }
        }
    }

    fun evaluate(command: RoboCommand, context: SafetyContext) {
        runOperation {
            val decision = repository.evaluateAndExecute(command, context)
            _uiState.update {
                it.copy(
                    lastDecision = decision,
                    lastCommandResult = null
                )
            }
        }
    }

    fun emergencyStop(reason: String = "Emergency stop requested by operator.") {
        runCommandOperation { repository.emergencyStop(reason) }
    }

    fun safePosition(reason: String = "Safe position requested by operator.") {
        runCommandOperation { repository.safePosition(reason) }
    }

    fun acknowledge(eventId: String) {
        runOperation { repository.acknowledge(eventId) }
    }

    private fun runCommandOperation(operation: suspend () -> CommandResult) {
        runOperation {
            val result = operation()
            _uiState.update {
                it.copy(
                    lastCommandResult = result,
                    error = if (result.accepted) null else result.reason
                        ?: "The device rejected the safety command."
                )
            }
        }
    }

    private fun runOperation(operation: suspend () -> Unit) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                operation()
                _uiState.update { it.copy(isLoading = false) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.localizedMessage
                            ?.takeIf { message -> message.isNotBlank() }
                            ?: "Safety operation failed."
                    )
                }
            }
        }
    }
}
