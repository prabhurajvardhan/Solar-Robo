package com.solarrobo.feature.home.data

import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel
import kotlinx.coroutines.flow.Flow

interface HomeDataSource {
    fun observeRoboSnapshot(): Flow<RoboSnapshot>
    fun observeEnergySnapshot(): Flow<EnergySnapshot>
    fun observeSafetyLevel(): Flow<SafetyLevel>
    fun observeActiveSafetyEvents(): Flow<List<SafetyEvent>>
    fun observeLatestRoboMessage(): Flow<String>
    suspend fun refresh()
}
