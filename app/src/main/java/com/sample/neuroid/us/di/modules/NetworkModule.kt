package com.sample.neuroid.us.di.modules

import com.sample.neuroid.us.data.network.NIDNetworkHelper
import com.sample.neuroid.us.data.network.NIDServices
import com.sample.neuroid.us.data.network.NetworkInteractor
import com.sample.neuroid.us.data.network.NetworkInteractorImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    fun providesNIDServices(networkHelper: NIDNetworkHelper): NIDServices {
        return networkHelper.service
    }

    @Provides
    @Singleton
    fun provideNetworkInteractor(networkInteractorImpl: NetworkInteractorImpl): NetworkInteractor {
        return networkInteractorImpl
    }

}