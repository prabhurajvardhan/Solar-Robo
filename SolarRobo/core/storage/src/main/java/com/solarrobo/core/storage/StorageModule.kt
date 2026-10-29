package com.solarrobo.core.storage

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    @Binds
    @Singleton
    abstract fun bindSecurePreferencesStore(
        implementation: EncryptedPreferencesDataStore
    ): SecurePreferencesStore

    @Binds
    @Singleton
    abstract fun bindPreferenceStorage(
        implementation: DataStorePreferenceStorage
    ): PreferenceStorage

    @Binds
    @Singleton
    abstract fun bindTrackerStateStore(
        implementation: DataStoreTrackerStateStore
    ): com.solarrobo.core.tracker.TrackerStateStore
}
