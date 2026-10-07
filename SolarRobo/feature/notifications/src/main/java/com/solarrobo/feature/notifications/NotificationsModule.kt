package com.solarrobo.feature.notifications

import com.solarrobo.feature.notifications.data.NotificationsRepositoryImpl
import com.solarrobo.feature.notifications.domain.NotificationsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {
    @Binds
    @Singleton
    abstract fun bindNotificationsRepository(
        implementation: NotificationsRepositoryImpl
    ): NotificationsRepository
}
