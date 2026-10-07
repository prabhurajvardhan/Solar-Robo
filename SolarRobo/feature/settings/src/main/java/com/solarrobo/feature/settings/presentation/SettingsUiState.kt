package com.solarrobo.feature.settings.presentation

import com.solarrobo.feature.settings.domain.UserSettings

data class SettingsUiState(
    val settings: UserSettings? = null,
    val isLoading: Boolean = false,
    val isOffline: Boolean = true,
    val errorMessage: String? = null
)
