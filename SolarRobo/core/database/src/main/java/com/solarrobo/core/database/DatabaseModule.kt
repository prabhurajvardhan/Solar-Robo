package com.solarrobo.core.database

import android.content.Context
import androidx.room.Room
import com.solarrobo.core.database.notifications.NotificationDao
import com.solarrobo.core.database.notifications.NotificationDatabase
import com.solarrobo.core.contracts.TrackerLogSink
import com.solarrobo.core.database.tracker.RoomTrackerLogSink
import com.solarrobo.core.database.tracker.TrackerLogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideNotificationDatabase(
        @ApplicationContext context: Context
    ): NotificationDatabase =
        Room.databaseBuilder(
            context,
            NotificationDatabase::class.java,
            "solar-robo-notifications.db"
        ).build()

    @Provides
    fun provideNotificationDao(database: NotificationDatabase): NotificationDao =
        database.notificationDao()

    @Provides
    @Singleton
    fun provideTrackerLogDatabase(@ApplicationContext context: Context): TrackerLogDatabase =
        Room.databaseBuilder(context, TrackerLogDatabase::class.java, "solar-robo-tracker.db").build()

    @Provides
    @Singleton
    fun provideTrackerLogSink(sink: RoomTrackerLogSink): TrackerLogSink = sink
}
