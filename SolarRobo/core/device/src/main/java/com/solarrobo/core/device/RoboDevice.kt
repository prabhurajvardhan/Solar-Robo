package com.solarrobo.core.device

import com.solarrobo.core.contracts.CommandResult
import com.solarrobo.core.contracts.RoboCommand
import com.solarrobo.core.contracts.RoboSnapshot
import kotlinx.coroutines.flow.Flow

interface RoboDevice {
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun getSnapshot(): RoboSnapshot
    suspend fun sendCommand(command: RoboCommand): CommandResult
    fun observeSnapshot(): Flow<RoboSnapshot>
}
