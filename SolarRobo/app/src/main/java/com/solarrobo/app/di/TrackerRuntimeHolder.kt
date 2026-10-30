package com.solarrobo.app.di

import com.solarrobo.core.contracts.TrackerEngine
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class TrackerRuntimeHolder @Inject constructor() {
    private val _engine = MutableStateFlow<TrackerEngine?>(null)
    val engine: StateFlow<TrackerEngine?> = _engine.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun attach(engine: TrackerEngine) { _error.value = null; _engine.value = engine }
    fun reportError(message: String) { _error.value = message }
    fun clear() { _engine.value = null }
}
