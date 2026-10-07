package com.solarrobo.feature.activity.presentation

import com.solarrobo.core.contracts.ActivityEvent
import com.solarrobo.core.contracts.ActivityType

data class ActivityUiState(
    val events: List<ActivityEvent> = emptyList(),
    val filterType: ActivityType? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
