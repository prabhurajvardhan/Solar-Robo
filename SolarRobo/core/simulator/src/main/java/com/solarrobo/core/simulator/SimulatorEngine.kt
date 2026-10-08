package com.solarrobo.core.simulator

import com.solarrobo.core.contracts.DeviceHealth
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import kotlinx.coroutines.flow.Flow

/**
 * Deterministic hardware and environmental simulation engine interface.
 * Serves as the primary mock backend for integration testing and preview flows.
 */
interface SimulatorEngine {
    val currentScenario: Flow<SimulationScenario>
    fun observeRoboSnapshot(): Flow<RoboSnapshot>
    fun observeEnergySnapshot(): Flow<EnergySnapshot>
    fun observeEnvironmentSnapshot(): Flow<EnvironmentSnapshot>
    fun observeDeviceHealth(): Flow<DeviceHealth>
    fun observeSafetyEvents(): Flow<List<SafetyEvent>>

    suspend fun setScenario(scenario: SimulationScenario)
    suspend fun injectFault(code: String, message: String): SafetyEvent
    suspend fun tick(deltaMillis: Long)
}
