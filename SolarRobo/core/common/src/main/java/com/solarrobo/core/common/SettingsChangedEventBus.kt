package com.solarrobo.core.common

import com.solarrobo.core.contracts.SettingsChangedEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsChangedEventBus {
    val events: Flow<SettingsChangedEvent>
    suspend fun publish(event: SettingsChangedEvent)
}

@Singleton
class InProcessSettingsChangedEventBus @Inject constructor() : SettingsChangedEventBus {
    private val mutableEvents = MutableSharedFlow<SettingsChangedEvent>(replay = 1)

    override val events: Flow<SettingsChangedEvent> = mutableEvents.asSharedFlow()

    override suspend fun publish(event: SettingsChangedEvent) {
        mutableEvents.emit(event)
    }
}
