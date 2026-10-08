package com.solarrobo.feature.home.domain

import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyEvent
import com.solarrobo.core.contracts.SafetyLevel
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun getRoboSnapshot(): Flow<RoboSnapshot>
    fun getEnergySnapshot(): Flow<EnergySnapshot>
    fun getSafetyLevel(): Flow<SafetyLevel>
    fun getActiveSafetyEvents(): Flow<List<SafetyEvent>>
    fun getLatestRoboMessage(): Flow<String>
    fun getErrorMessage(): Flow<String?>
    suspend fun refresh()
}
