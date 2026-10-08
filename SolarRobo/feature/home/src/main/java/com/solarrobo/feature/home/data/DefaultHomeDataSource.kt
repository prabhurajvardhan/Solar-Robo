package com.solarrobo.feature.home.data

import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.home.mock.FakeHomeRepository
import com.solarrobo.feature.home.mock.HomeScenario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultHomeDataSource @Inject constructor() : HomeDataSource {

    private val _robo = MutableStateFlow(
        FakeHomeRepository.createRoboSnapshotForScenario(HomeScenario.NORMAL)
    )
    private val _energy = MutableStateFlow(
        FakeHomeRepository.createEnergySnapshotForScenario(HomeScenario.NORMAL)
    )
    private val _safety = MutableStateFlow(
        FakeHomeRepository.createSafetyLevelForScenario(HomeScenario.NORMAL)
    )
    private val _safetyEvents = MutableStateFlow(
        FakeHomeRepository.createSafetyEventsForScenario(HomeScenario.NORMAL)
    )
    private val _message = MutableStateFlow(
        FakeHomeRepository.createRoboMessageForScenario(HomeScenario.NORMAL)
    )

    override fun observeRoboSnapshot(): Flow<RoboSnapshot> = _robo.asStateFlow()

    override fun observeEnergySnapshot(): Flow<EnergySnapshot> = _energy.asStateFlow()

    override fun observeSafetyLevel(): Flow<SafetyLevel> = _safety.asStateFlow()

    override fun observeActiveSafetyEvents(): Flow<List<SafetyEvent>> = _safetyEvents.asStateFlow()

    override fun observeLatestRoboMessage(): Flow<String> = _message.asStateFlow()

    override suspend fun refresh() {
        val now = System.currentTimeMillis()
        _robo.value = _robo.value.copy(timestamp = now)
        _energy.value = _energy.value.copy(timestamp = now)
    }
}
