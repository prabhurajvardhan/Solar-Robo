package com.solarrobo.feature.energy

import com.solarrobo.feature.energy.data.DefaultEnergyDataSource
import com.solarrobo.feature.energy.data.EnergyDataSource
import com.solarrobo.feature.energy.data.EnergyRepositoryImpl
import com.solarrobo.feature.energy.domain.EnergyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EnergyModule {

    @Binds
    @Singleton
    abstract fun bindEnergyDataSource(
        implementation: DefaultEnergyDataSource
    ): EnergyDataSource

    @Binds
    @Singleton
    abstract fun bindEnergyRepository(
        implementation: EnergyRepositoryImpl
    ): EnergyRepository
}
