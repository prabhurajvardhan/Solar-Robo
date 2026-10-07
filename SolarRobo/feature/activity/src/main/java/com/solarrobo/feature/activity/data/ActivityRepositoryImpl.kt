package com.solarrobo.feature.activity.data

import com.solarrobo.core.contracts.ActivityEvent
import com.solarrobo.core.contracts.ActivityType
import com.solarrobo.feature.activity.domain.ActivityFilter
import com.solarrobo.feature.activity.domain.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor() : ActivityRepository {
    private val memoryEvents = MutableStateFlow(
        listOf(
            ActivityEvent(
                "1",
                ActivityType.MOVEMENT,
                "Sun Tracking Started",
                "Tracking solar elevation at 42°",
                System.currentTimeMillis() - 3600000L
            ),
            ActivityEvent(
                "2",
                ActivityType.ENERGY,
                "Peak Generation Hit",
                "Generated 340W at 12:00 PM",
                System.currentTimeMillis() - 1800000L
            )
        )
    )

    override fun observeEvents(filter: ActivityFilter): Flow<List<ActivityEvent>> {
        return memoryEvents.map { events ->
            val filtered = if (filter.selectedType == null) {
                events
            } else {
                events.filter { it.type == filter.selectedType }
            }
            filtered.sortedByDescending { it.timestamp }
        }
    }

    override suspend fun recordEvent(event: ActivityEvent) {
        memoryEvents.value = listOf(event) + memoryEvents.value
    }
}
