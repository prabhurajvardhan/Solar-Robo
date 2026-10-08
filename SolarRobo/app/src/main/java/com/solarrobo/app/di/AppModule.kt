package com.solarrobo.app.di

import com.solarrobo.core.ai.AiEngine
import com.solarrobo.core.ai.FakeAiEngine
import com.solarrobo.core.camera.CameraProvider
import com.solarrobo.core.camera.FakeCameraProvider
import com.solarrobo.core.common.AppDispatchers
import com.solarrobo.core.common.DefaultAppDispatchers
import com.solarrobo.core.common.SystemTimeProvider
import com.solarrobo.core.common.TimeProvider
import com.solarrobo.core.database.DatabaseProvider
import com.solarrobo.core.database.InMemoryDatabaseProvider
import com.solarrobo.core.device.FakeRoboDevice
import com.solarrobo.core.device.RoboDevice
import com.solarrobo.core.network.FakeNetworkMonitor
import com.solarrobo.core.network.NetworkMonitor
import com.solarrobo.core.simulator.SimulatorEngine
import com.solarrobo.core.simulator.SimulatorEngineImpl
import com.solarrobo.core.storage.InMemoryPreferenceStorage
import com.solarrobo.core.storage.PreferenceStorage
import dagger.Module
import dagger.Provides
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
    fun provideRoboDevice(): RoboDevice = FakeRoboDevice()

    @Provides
    @Singleton
    fun provideSimulatorEngine(): SimulatorEngine = SimulatorEngineImpl()

    @Provides
    @Singleton
    fun provideAiEngine(): AiEngine = FakeAiEngine()

    @Provides
    @Singleton
    fun provideCameraProvider(): CameraProvider = FakeCameraProvider()

    @Provides
    @Singleton
    fun provideNetworkMonitor(): NetworkMonitor = FakeNetworkMonitor()

    @Provides
    @Singleton
    fun provideDatabaseProvider(): DatabaseProvider = InMemoryDatabaseProvider()

    @Provides
    @Singleton
    fun providePreferenceStorage(): PreferenceStorage = InMemoryPreferenceStorage()
}
