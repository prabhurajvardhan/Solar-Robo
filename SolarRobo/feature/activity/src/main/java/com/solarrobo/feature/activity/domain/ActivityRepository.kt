package com.solarrobo.feature.activity.domain

import com.solarrobo.core.contracts.ActivityEvent
import com.solarrobo.core.contracts.ActivityType
import kotlinx.coroutines.flow.Flow

data class ActivityFilter(
    val selectedType: ActivityType? = null
)

interface ActivityRepository {
    fun observeEvents(filter: ActivityFilter): Flow<List<ActivityEvent>>
    suspend fun recordEvent(event: ActivityEvent)
}
