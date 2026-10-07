package com.solarrobo.feature.activity.mock

import com.solarrobo.core.contracts.ActivityEvent
import com.solarrobo.core.contracts.ActivityType
import com.solarrobo.feature.activity.domain.ActivityFilter
import com.solarrobo.feature.activity.domain.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeActivityRepository : ActivityRepository {
    private val events = MutableStateFlow(
        listOf(
            ActivityEvent(
                id = "preview-1",
                type = ActivityType.MOVEMENT,
                title = "Panel adjusted",
                detail = "Tracking moved to sunrise alignment",
                timestamp = System.currentTimeMillis() - 4_000L
            ),
            ActivityEvent(
                id = "preview-2",
                type = ActivityType.SAFETY,
                title = "Safety gate checked",
                detail = "Wind conditions are within safe limits",
                timestamp = System.currentTimeMillis() - 2_000L
            )
        )
    )

    override fun observeEvents(filter: ActivityFilter): Flow<List<ActivityEvent>> {
        return events.map { list ->
            val filtered = if (filter.selectedType == null) list else list.filter { it.type == filter.selectedType }
            filtered.sortedByDescending { it.timestamp }
        }
    }

    override suspend fun recordEvent(event: ActivityEvent) {
        events.value = listOf(event) + events.value
    }
}
