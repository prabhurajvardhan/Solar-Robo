package com.solarrobo.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solarrobo.app.weather.WeatherRuntime
import com.solarrobo.app.weather.WeatherUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class WeatherViewModel @Inject constructor(private val runtime: WeatherRuntime) : ViewModel() {
    val uiState: StateFlow<WeatherUiState> = runtime.state
    init {
        runtime.refreshForecast()
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                runtime.updateFreshness()
            }
        }
    }
    fun refresh() = runtime.refreshForecast()
}
