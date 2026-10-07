package com.solarrobo.feature.safety

import com.solarrobo.feature.safety.data.SafetyRepositoryImpl
import com.solarrobo.feature.safety.domain.SafetyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SafetyModule {
    @Binds
    @Singleton
    abstract fun bindSafetyRepository(implementation: SafetyRepositoryImpl): SafetyRepository
}
