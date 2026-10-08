package com.solarrobo.core.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Key-value asynchronous storage abstraction.
 */
interface PreferenceStorage {
    fun getBoolean(key: String, defaultValue: Boolean = false): Flow<Boolean>
    suspend fun setBoolean(key: String, value: Boolean)
    fun getString(key: String, defaultValue: String = ""): Flow<String>
    suspend fun setString(key: String, value: String)
}

class InMemoryPreferenceStorage : PreferenceStorage {
    private val booleanStore = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val stringStore = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun getBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        booleanStore.map { it[key] ?: defaultValue }

    override suspend fun setBoolean(key: String, value: Boolean) {
        booleanStore.value = booleanStore.value + (key to value)
    }

    override fun getString(key: String, defaultValue: String): Flow<String> =
        stringStore.map { it[key] ?: defaultValue }

    override suspend fun setString(key: String, value: String) {
        stringStore.value = stringStore.value + (key to value)
    }
}
