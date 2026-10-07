package com.solarrobo.core.storage

import kotlinx.coroutines.flow.Flow

interface SecurePreferencesStore {
    val preferences: Flow<Map<String, String>>
    suspend fun update(transform: (Map<String, String>) -> Map<String, String>)
}
