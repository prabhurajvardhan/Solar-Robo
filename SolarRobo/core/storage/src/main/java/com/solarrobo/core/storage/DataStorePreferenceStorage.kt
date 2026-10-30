package com.solarrobo.core.storage

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DataStorePreferenceStorage @Inject constructor(
    private val store: SecurePreferencesStore
) : PreferenceStorage {
    override fun getBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        store.preferences.map { it[key]?.toBooleanStrictOrNull() ?: defaultValue }

    override suspend fun setBoolean(key: String, value: Boolean) {
        store.update { current -> current + (key to value.toString()) }
    }

    override fun getString(key: String, defaultValue: String): Flow<String> =
        store.preferences.map { it[key] ?: defaultValue }

    override suspend fun setString(key: String, value: String) {
        store.update { current -> current + (key to value) }
    }
}
