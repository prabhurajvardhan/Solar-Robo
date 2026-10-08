package com.solarrobo.feature.energy.data

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import com.solarrobo.feature.energy.domain.EnergyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnergyRepositoryImpl @Inject constructor(
    private val dataSource: EnergyDataSource
) : EnergyRepository {

    private val _errorMessage = MutableStateFlow<String?>(null)

    override fun getEnergySnapshot(): Flow<EnergySnapshot> = dataSource.observeEnergySnapshot()

    override fun getEnergyHistory(): Flow<List<EnergyHistoryPoint>> = dataSource.observeEnergyHistory()

    override fun getErrorMessage(): Flow<String?> = _errorMessage.asStateFlow()

    override suspend fun refresh() {
        try {
            dataSource.refresh()
            _errorMessage.value = null
        } catch (t: Throwable) {
            _errorMessage.value = t.message ?: "Failed to refresh energy telemetry."
        }
    }
}
