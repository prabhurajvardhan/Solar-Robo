package com.solarrobo.feature.auth.di

import com.solarrobo.feature.auth.data.DataStoreAuthRepository
import com.solarrobo.feature.auth.domain.AuthClock
import com.solarrobo.feature.auth.domain.AuthRepository
import com.solarrobo.feature.auth.domain.SystemAuthClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(implementation: DataStoreAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuthClock(implementation: SystemAuthClock): AuthClock
}
