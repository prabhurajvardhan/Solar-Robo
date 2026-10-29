package com.solarrobo.app.di

import com.solarrobo.core.camera.CameraXFrameSource
import com.solarrobo.core.common.AppDispatchers
import com.solarrobo.core.common.DefaultAppDispatchers
import com.solarrobo.core.common.SystemTimeProvider
import com.solarrobo.core.common.TimeProvider
import com.solarrobo.core.database.DatabaseProvider
import com.solarrobo.core.database.InMemoryDatabaseProvider
import android.content.Context
import com.solarrobo.core.device.Esp32WifiDevice
import com.solarrobo.core.device.RoboDevice
import com.solarrobo.core.tracker.TrackerSafety
import com.solarrobo.core.network.FakeNetworkMonitor
import com.solarrobo.core.network.NetworkMonitor
import com.solarrobo.core.simulator.SimulatorEngine
import com.solarrobo.core.simulator.SimulatorEngineImpl
import com.solarrobo.core.contracts.SafetyGate
import com.solarrobo.core.contracts.SafetyLatchStore
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = DefaultAppDispatchers()

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()

    @Provides
    @Singleton
    fun provideEsp32WifiDevice(@ApplicationContext context: Context): Esp32WifiDevice = Esp32WifiDevice(context)

    @Provides
    @Singleton
    fun provideRoboDevice(device: Esp32WifiDevice): RoboDevice = device

    @Provides
    @Singleton
    fun provideTrackerSafety(gate: SafetyGate): TrackerSafety = SafetyGateTrackerSafety(gate)

    @Provides
    @Singleton
    fun provideCameraXFrameSource(@ApplicationContext context: Context): CameraXFrameSource = CameraXFrameSource(context)

    @Provides
    @Singleton
    fun provideSafetyGate(@ApplicationContext context: Context): SafetyGate {
        val preferences = context.getSharedPreferences("solar_robo_safety", Context.MODE_PRIVATE)
        val latchStore = object : SafetyLatchStore {
            override fun isLatched(): Boolean = preferences.getBoolean("emergency_stop_latched", false)
            override fun setLatched(latched: Boolean) {
                check(preferences.edit().putBoolean("emergency_stop_latched", latched).commit()) {
                    "Could not persist emergency-stop latch"
                }
            }
        }
        return SafetyGate(latchStore)
    }

    @Provides
    @Singleton
    fun provideSimulatorEngine(): SimulatorEngine = SimulatorEngineImpl()

    @Provides
    @Singleton
    fun provideNetworkMonitor(): NetworkMonitor = FakeNetworkMonitor()

    @Provides
    @Singleton
    fun provideDatabaseProvider(): DatabaseProvider = InMemoryDatabaseProvider()

}
