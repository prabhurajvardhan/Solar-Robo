package com.solarrobo.core.common

import com.solarrobo.core.contracts.NotificationEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationEventBus {
    val events: Flow<NotificationEvent>
    suspend fun publish(event: NotificationEvent)
}

@Singleton
class InProcessNotificationEventBus @Inject constructor() : NotificationEventBus {
    private val channel = Channel<NotificationEvent>(Channel.BUFFERED)

    override val events: Flow<NotificationEvent> = channel.receiveAsFlow()

    override suspend fun publish(event: NotificationEvent) {
        channel.send(event)
    }
}
