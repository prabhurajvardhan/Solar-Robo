package com.solarrobo.feature.auth.data

import com.solarrobo.core.database.DatabaseProvider
import com.solarrobo.core.storage.SecurePreferencesStore
import com.solarrobo.feature.auth.domain.AuthClock
import com.solarrobo.feature.auth.domain.PinHasher
import com.solarrobo.feature.auth.domain.PinVerification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreAuthRepositoryTest {
    @Test
    fun wrongPinLockoutPersistsAndDoublesWithFakeClock() = runBlocking {
        val store = MemorySecureStore()
        val clock = MutableClock()
        val repository = repository(store, clock)
        repository.createProfile("Operator", "123456", "123456", false)
        assertTrue(store.values.value.values.none { it == "123456" })

        repeat(4) { assertTrue(repository.verifyPin("000000") is PinVerification.Rejected) }
        assertEquals(PinVerification.Locked(30_000L), repository.verifyPin("000000"))
        assertEquals(30_000L, repository(store, clock).remainingLockoutMillis())
        assertEquals(PinVerification.Locked(30_000L), repository.verifyPin("123456"))

        clock.now += 30_000L
        assertEquals(PinVerification.Locked(60_000L), repository.verifyPin("000000"))
        clock.now += 60_000L
        assertEquals(PinVerification.Accepted, repository.verifyPin("123456"))
        assertEquals("0", store.values.value["auth.pin.failed_attempts"])
    }

    @Test
    fun authenticatedResetClearsPreferencesAndDatabase() = runBlocking {
        val store = MemorySecureStore()
        var cleared = false
        val database = object : DatabaseProvider {
            override val isInitialized = true
            override suspend fun clearAllTables() { cleared = true }
        }
        val repository = DataStoreAuthRepository(store, database, PinHasher(), MutableClock())
        repository.createProfile("Operator", "123456", "123456", false)
        assertEquals(PinVerification.Accepted, repository.resetProfile("123456"))
        assertTrue(cleared)
        assertTrue(store.values.value.isEmpty())
        assertEquals(null, repository.profile.first())
    }

    private fun repository(store: MemorySecureStore, clock: MutableClock) = DataStoreAuthRepository(
        store,
        object : DatabaseProvider {
            override val isInitialized = true
            override suspend fun clearAllTables() = Unit
        },
        PinHasher(),
        clock
    )

    private class MutableClock(var now: Long = 100_000L) : AuthClock {
        override fun nowMillis() = now
    }

    private class MemorySecureStore : SecurePreferencesStore {
        val values = MutableStateFlow<Map<String, String>>(emptyMap())
        override val preferences = values
        override suspend fun update(transform: (Map<String, String>) -> Map<String, String>) {
            values.value = transform(values.value)
        }
    }
}
