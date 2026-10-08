package com.solarrobo.feature.home

import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.home.data.DefaultHomeDataSource
import com.solarrobo.feature.home.data.HomeRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRepositoryTest {

    @Test
    fun repository_observesDefaultDataSuccessfully() = runTest {
        val dataSource = DefaultHomeDataSource()
        val repository = HomeRepositoryImpl(dataSource)

        val robo = repository.getRoboSnapshot().first()
        val energy = repository.getEnergySnapshot().first()
        val safety = repository.getSafetyLevel().first()
        val message = repository.getLatestRoboMessage().first()
        val error = repository.getErrorMessage().first()

        assertNotNull(robo)
        assertTrue(robo.connected)
        assertNotNull(energy)
        assertEquals(SafetyLevel.NORMAL, safety)
        assertEquals("Solar generation is stable.", message)
        assertNull(error)
    }

    @Test
    fun repository_refreshExecutesWithoutCrashing() = runTest {
        val dataSource = DefaultHomeDataSource()
        val repository = HomeRepositoryImpl(dataSource)

        repository.refresh()
        val error = repository.getErrorMessage().first()
        assertNull(error)
    }
}
