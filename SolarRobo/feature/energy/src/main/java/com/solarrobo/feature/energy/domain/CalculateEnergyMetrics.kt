package com.solarrobo.feature.energy.domain

import com.solarrobo.core.contracts.EnergyHistoryPoint
import com.solarrobo.core.contracts.EnergySnapshot
import javax.inject.Inject

enum class EnergyStatus {
    HIGH_GENERATION,
    GENERATING,
    LOW_GENERATION,
    BATTERY_CHARGING,
    BATTERY_DISCHARGING,
    ENERGY_CONSERVATION
}

data class EnergySummaryMetrics(
    val status: EnergyStatus,
    val netPowerWatts: Float,
    val isCharging: Boolean,
    val totalHistoricalYieldWh: Float,
    val averageSolarEfficiency: Float
)

class CalculateEnergyMetrics @Inject constructor() {
    operator fun invoke(
        snapshot: EnergySnapshot,
        history: List<EnergyHistoryPoint>
    ): EnergySummaryMetrics {
        val netPower = snapshot.generatedWatts - snapshot.consumedWatts
        val isCharging = snapshot.batteryPowerWatts > 0f

        val status = when {
            snapshot.batteryPercent <= snapshot.reservePercent -> EnergyStatus.ENERGY_CONSERVATION
            snapshot.generatedWatts >= 400f -> EnergyStatus.HIGH_GENERATION
            snapshot.generatedWatts in 100f..399f -> EnergyStatus.GENERATING
            snapshot.generatedWatts > 0f && snapshot.generatedWatts < 100f -> EnergyStatus.LOW_GENERATION
            isCharging -> EnergyStatus.BATTERY_CHARGING
            else -> EnergyStatus.BATTERY_DISCHARGING
        }

        val totalYield = history.sumOf { it.generatedWh.toDouble() }.toFloat()
        val avgEfficiency = if (history.isNotEmpty()) {
            (history.sumOf { it.solarEfficiency.toDouble() } / history.size).toFloat()
        } else {
            0f
        }

        return EnergySummaryMetrics(
            status = status,
            netPowerWatts = netPower,
            isCharging = isCharging,
            totalHistoricalYieldWh = totalYield,
            averageSolarEfficiency = avgEfficiency
        )
    }
}
