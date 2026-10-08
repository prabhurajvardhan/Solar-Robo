package com.solarrobo.feature.home.data

import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.core.contracts.RoboSnapshot
import com.solarrobo.core.contracts.SafetyLevel
import com.solarrobo.feature.home.domain.HomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val dataSource: HomeDataSource
) : HomeRepository {

    private val _errorMessage = MutableStateFlow<String?>(null)

    override fun getRoboSnapshot(): Flow<RoboSnapshot> = dataSource.observeRoboSnapshot()

    override fun getEnergySnapshot(): Flow<EnergySnapshot> = dataSource.observeEnergySnapshot()

    override fun getSafetyLevel(): Flow<SafetyLevel> = dataSource.observeSafetyLevel()

    override fun getActiveSafetyEvents(): Flow<List<com.solarrobo.core.contracts.SafetyEvent>> = dataSource.observeActiveSafetyEvents()

    override fun getLatestRoboMessage(): Flow<String> = dataSource.observeLatestRoboMessage()

    override fun getErrorMessage(): Flow<String?> = _errorMessage.asStateFlow()

    override suspend fun refresh() {
        try {
            dataSource.refresh()
            _errorMessage.value = null
        } catch (t: Throwable) {
            _errorMessage.value = t.message ?: "Failed to refresh command center data."
        }
    }
}
