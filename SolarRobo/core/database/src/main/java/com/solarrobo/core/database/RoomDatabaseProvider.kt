package com.solarrobo.core.database

import com.solarrobo.core.database.notifications.NotificationDatabase
import com.solarrobo.core.database.tracker.TrackerLogDatabase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RoomDatabaseProvider @Inject constructor(
    private val notifications: NotificationDatabase,
    private val trackerLogs: TrackerLogDatabase
) : DatabaseProvider {
    override val isInitialized: Boolean = true

    override suspend fun clearAllTables() = withContext(Dispatchers.IO) {
        notifications.clearAllTables()
        trackerLogs.clearAllTables()
    }
}
