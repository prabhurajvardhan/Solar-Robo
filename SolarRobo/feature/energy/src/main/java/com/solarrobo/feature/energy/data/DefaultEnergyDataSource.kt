package com.solarrobo.feature.energy.data

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.feature.energy.mock.EnergyScenario
import com.solarrobo.feature.energy.mock.FakeEnergyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultEnergyDataSource @Inject constructor() : EnergyDataSource {

    private val _snapshot = MutableStateFlow(
        FakeEnergyRepository.createSnapshotForScenario(EnergyScenario.NORMAL)
    )
    private val _history = MutableStateFlow(
        FakeEnergyRepository.createHistoryForScenario(EnergyScenario.NORMAL)
    )

    override fun observeEnergySnapshot(): Flow<EnergySnapshot> = _snapshot.asStateFlow()

    override fun observeEnergyHistory(): Flow<List<EnergyHistoryPoint>> = _history.asStateFlow()

    override suspend fun refresh() {
        // Keeps realistic snapshot state updated
        _snapshot.value = _snapshot.value.copy(timestamp = System.currentTimeMillis())
    }
}
