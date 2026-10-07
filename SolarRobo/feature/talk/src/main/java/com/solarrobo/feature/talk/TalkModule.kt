package com.solarrobo.feature.talk

import com.solarrobo.core.ai.AiEngine
import com.solarrobo.feature.talk.data.TalkRepositoryImpl
import com.solarrobo.feature.talk.domain.TalkRepository
import com.solarrobo.feature.talk.mock.MockAiEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TalkModule {
    @Binds
    @Singleton
    abstract fun bindTalkRepository(implementation: TalkRepositoryImpl): TalkRepository

    @Binds
    @Singleton
    abstract fun bindAiEngine(implementation: MockAiEngine): AiEngine
}