package com.solarrobo.feature.notifications.presentation

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.feature.notifications.domain.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationsRepository
) : ViewModel() {
    private val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val uiState = retryRequests
        .onStart { emit(Unit) }
        .flatMapLatest {
            combine(
                repository.getNotifications(),
                repository.getUnreadCount(),
                repository.getErrorMessage()
            ) { notifications, unreadCount, errorMessage ->
                NotificationsUiState(
                    notifications = notifications,
                    unreadCount = unreadCount,
                    isOffline = true,
                    errorMessage = errorMessage
                )
            }.onStart {
                emit(NotificationsUiState(isLoading = true))
            }.catch { error ->
                if (error is SQLiteException || error is IllegalArgumentException) {
                    emit(
                        NotificationsUiState(
                            isOffline = true,
                            errorMessage = error.message ?: "Notifications are unavailable."
                        )
                    )
                } else {
                    throw error
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationsUiState(isLoading = true)
        )

    fun markRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun retry() {
        retryRequests.tryEmit(Unit)
    }
}
