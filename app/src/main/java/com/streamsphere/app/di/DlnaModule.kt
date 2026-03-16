package com.streamsphere.app.di

import com.streamsphere.app.data.dlna.DlnaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DlnaModule {

    @Provides
    @Singleton
    fun provideDlnaRepository(): DlnaRepository = DlnaRepository()
}
