package com.solarrobo.feature.talk

import android.content.Context
import com.solarrobo.core.ai.AiEngine
import com.solarrobo.core.ai.ModelAwareSolarAiEngine
import com.solarrobo.core.ai.OnDeviceSolarAiEngine
import com.solarrobo.core.ai.TernaryModelRepository
import com.solarrobo.feature.talk.data.TalkRepositoryImpl
import com.solarrobo.feature.talk.domain.TalkRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TalkModule {
    @Binds
    @Singleton
    abstract fun bindTalkRepository(implementation: TalkRepositoryImpl): TalkRepository

    companion object {
        @Provides
        @Singleton
        fun provideTernaryModelRepository(@ApplicationContext context: Context): TernaryModelRepository =
            TernaryModelRepository(context)

        @Provides
        @Singleton
        fun provideAiEngine(
            @ApplicationContext context: Context,
            models: TernaryModelRepository,
            fallback: OnDeviceSolarAiEngine
        ): AiEngine = ModelAwareSolarAiEngine(context, models, fallback)
    }
}
