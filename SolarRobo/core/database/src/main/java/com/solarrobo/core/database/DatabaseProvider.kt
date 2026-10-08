package com.solarrobo.core.database

/**
 * Foundation database provider contract for persistence layers.
 */
interface DatabaseProvider {
    val isInitialized: Boolean
    suspend fun clearAllTables()
}

class InMemoryDatabaseProvider : DatabaseProvider {
    override var isInitialized: Boolean = true

    override suspend fun clearAllTables() {
        // No-op in-memory stub
    }
}
