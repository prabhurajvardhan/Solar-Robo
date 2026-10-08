package com.solarrobo.feature.home

import com.solarrobo.feature.home.data.DefaultHomeDataSource
import com.solarrobo.feature.home.data.HomeDataSource
import com.solarrobo.feature.home.data.HomeRepositoryImpl
import com.solarrobo.feature.home.domain.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {

    @Binds
    @Singleton
    abstract fun bindHomeDataSource(
        impl: DefaultHomeDataSource
    ): HomeDataSource

    @Binds
    @Singleton
    abstract fun bindHomeRepository(
        impl: HomeRepositoryImpl
    ): HomeRepository
}
