package com.solarrobo.feature.simulator.mock

import com.solarrobo.core.contracts.DeviceHealth
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.EnvironmentSnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.feature.simulator.data.SimulatorRepositoryImpl
import com.solarrobo.feature.simulator.domain.Scenario
import com.solarrobo.feature.simulator.domain.SimulatorRepository
import kotlinx.coroutines.flow.Flow

class FakeSimulatorRepository : SimulatorRepository {
    private val simulator = SimulatorRepositoryImpl()

    override fun getRoboSnapshot(): Flow<RoboSnapshot> = simulator.getRoboSnapshot()
    override fun getEnergySnapshot(): Flow<EnergySnapshot> = simulator.getEnergySnapshot()
    override fun getEnvironmentSnapshot(): Flow<EnvironmentSnapshot> = simulator.getEnvironmentSnapshot()
    override fun getDeviceHealth(): Flow<DeviceHealth> = simulator.getDeviceHealth()
    override fun getSafetyEvents(): Flow<List<SafetyEvent>> = simulator.getSafetyEvents()
    override suspend fun applyScenario(scenario: Scenario) = simulator.applyScenario(scenario)
    override suspend fun tick(deltaMillis: Long) = simulator.tick(deltaMillis)
    override suspend fun injectFault(code: String, message: String): SafetyEvent =
        simulator.injectFault(code, message)
}