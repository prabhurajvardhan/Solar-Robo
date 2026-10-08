package com.solarrobo.core.device

import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.RoboMode
import com.solarrobo.core.contracts.RoboSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory simulated device implementation satisfying the RoboDevice contract.
 */
class FakeRoboDevice(
    initialConnected: Boolean = true,
    initialMode: RoboMode = RoboMode.NORMAL,
    initialAngleDeg: Float = 45.0f,
    initialWatts: Float = 320.0f,
    initialBattery: Float = 94.0f
) : RoboDevice {

    private val _snapshot = MutableStateFlow(
        RoboSnapshot(
            deviceId = "SOLAR-ROBO-SIM-01",
            name = "Solar Robo Unit",
            connected = initialConnected,
            mode = initialMode,
            panelAngleDeg = initialAngleDeg,
            targetAngleDeg = initialAngleDeg,
            generationWatts = initialWatts,
            batteryPercent = initialBattery,
            timestamp = System.currentTimeMillis()
        )
    )

    override suspend fun connect(): Result<Unit> {
        _snapshot.update { it.copy(connected = true, timestamp = System.currentTimeMillis()) }
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        _snapshot.update { it.copy(connected = false, timestamp = System.currentTimeMillis()) }
    }

    override suspend fun getSnapshot(): RoboSnapshot = _snapshot.value

    override suspend fun sendCommand(command: RoboCommand): CommandResult {
        return when (command) {
            is RoboCommand.MoveToAngle -> {
                val clamped = command.angleDeg.coerceIn(-90f, 90f)
                _snapshot.update {
                    it.copy(
                        panelAngleDeg = clamped,
                        targetAngleDeg = clamped,
                        timestamp = System.currentTimeMillis()
                    )
                }
                CommandResult(
                    accepted = true,
                    commandId = "CMD-${System.currentTimeMillis()}"
                )
            }
            RoboCommand.StopMotion -> {
                CommandResult(
                    accepted = true,
                    commandId = "CMD-${System.currentTimeMillis()}"
                )
            }
            RoboCommand.SafePosition -> {
                _snapshot.update {
                    it.copy(
                        panelAngleDeg = 0f,
                        targetAngleDeg = 0f,
                        mode = RoboMode.SAFE,
                        timestamp = System.currentTimeMillis()
                    )
                }
                CommandResult(
                    accepted = true,
                    commandId = "CMD-${System.currentTimeMillis()}"
                )
            }
        }
    }

    override fun observeSnapshot(): Flow<RoboSnapshot> = _snapshot.asStateFlow()

    fun updateTelemetry(generationWatts: Float, batteryPercent: Float, mode: RoboMode) {
        _snapshot.update {
            it.copy(
                generationWatts = generationWatts,
                batteryPercent = batteryPercent,
                mode = mode,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
