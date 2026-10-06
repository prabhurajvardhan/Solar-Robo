package com.solarrobo.feature.simulator

import com.solarrobo.feature.simulator.data.SimulatorRepositoryImpl
import com.solarrobo.feature.simulator.domain.SimulatorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SimulatorModule {
    @Binds
    @Singleton
    abstract fun bindSimulatorRepository(impl: SimulatorRepositoryImpl): SimulatorRepository
}