package com.solarrobo.feature.simulator.domain

import com.solarrobo.core.contracts.DeviceHealth
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import kotlinx.coroutines.flow.Flow

interface SimulatorRepository {
    fun getRoboSnapshot(): Flow<RoboSnapshot>
    fun getEnergySnapshot(): Flow<EnergySnapshot>
    fun getEnvironmentSnapshot(): Flow<EnvironmentSnapshot>
    fun getDeviceHealth(): Flow<DeviceHealth>
    fun getSafetyEvents(): Flow<List<SafetyEvent>>
    suspend fun applyScenario(scenario: Scenario)
    suspend fun tick(deltaMillis: Long)
    suspend fun injectFault(code: String, message: String): SafetyEvent
}