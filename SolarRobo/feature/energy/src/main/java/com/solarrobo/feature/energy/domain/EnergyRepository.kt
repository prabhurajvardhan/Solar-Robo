package com.solarrobo.feature.energy.domain

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import kotlinx.coroutines.flow.Flow

interface EnergyRepository {
    fun getEnergySnapshot(): Flow<EnergySnapshot>
    fun getEnergyHistory(): Flow<List<EnergyHistoryPoint>>
    fun getErrorMessage(): Flow<String?>
    suspend fun refresh()
}
