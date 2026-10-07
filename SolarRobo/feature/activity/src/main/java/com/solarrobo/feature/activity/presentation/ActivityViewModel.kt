package com.solarrobo.feature.activity.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.core.contracts.ActivityType
import com.solarrobo.feature.activity.domain.ActivityFilter
import com.solarrobo.feature.activity.domain.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val repository: ActivityRepository
) : ViewModel() {

    private val filterState = MutableStateFlow<ActivityType?>(null)

    val uiState: StateFlow<ActivityUiState> = filterState
        .flatMapLatest { type ->
            repository.observeEvents(ActivityFilter(type)).map { events ->
                ActivityUiState(
                    events = events,
                    filterType = type,
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActivityUiState(isLoading = true)
        )

    fun setFilter(type: ActivityType?) {
        filterState.value = type
    }
}
