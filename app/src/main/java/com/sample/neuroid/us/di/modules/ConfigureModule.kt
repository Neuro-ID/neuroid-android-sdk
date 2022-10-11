package com.sample.neuroid.us.di.modules

import com.sample.neuroid.us.domain.config.ConfigHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ConfigureModule {

    @Provides
    @Singleton
    fun provideConfigHelper(): ConfigHelper {
        return ConfigHelper()
    }
}