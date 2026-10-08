package com.solarrobo.feature.energy.data

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import kotlinx.coroutines.flow.Flow

interface EnergyDataSource {
    fun observeEnergySnapshot(): Flow<EnergySnapshot>
    fun observeEnergyHistory(): Flow<List<EnergyHistoryPoint>>
    suspend fun refresh()
}
