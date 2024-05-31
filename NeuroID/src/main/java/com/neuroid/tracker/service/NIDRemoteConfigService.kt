package com.neuroid.tracker.service

import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.models.NIDRemoteConfig
import com.neuroid.tracker.utils.NIDLogWrapper
import com.neuroid.tracker.utils.getRetroFitInstance
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class NIDRemoteConfigService(private var dispatcher: CoroutineDispatcher,
                             private var logger: NIDLogWrapper,
                             private val clientKey: String) {

    private var remoteNIDRemoteConfig: NIDRemoteConfig = NIDRemoteConfig()
    private var lastUpdateTime = 0L

    fun getRemoteNIDConfig(): NIDRemoteConfig {
        return try {
            val currentTime = Calendar.getInstance().timeInMillis
            if ((currentTime - lastUpdateTime) > LAST_UPDATE_INTERVAL) {
                setRemoteConfig()
            }
            remoteNIDRemoteConfig
        } catch (e: Exception) {
            remoteNIDRemoteConfig
        }
    }

    private fun setRemoteConfig() =
        runBlocking {
            val deferred =
                CoroutineScope(dispatcher).async {
                    NIDConfigurationService(
                        getRetroFitInstance(
                            NeuroID.scriptEndpoint,
                            logger,
                            NIDApiService::class.java,
                            remoteNIDRemoteConfig.requestTimeout
                        ),
                        object : OnRemoteConfigReceivedListener {
                            override fun onRemoteConfigReceived(remoteConfig: NIDRemoteConfig) {
                                remoteNIDRemoteConfig = remoteConfig
                                logger.d(msg = "remoteConfig: $remoteConfig")
                                lastUpdateTime = Calendar.getInstance().timeInMillis
                            }

                            override fun onRemoteConfigReceivedFailed(errorMessage: String) {
                                logger.e(msg = "error getting remote config: $errorMessage")
                            }
                        },
                        clientKey,
                    )
                }
            deferred.await()
        }

    companion object{
        // 24 hours
        const val LAST_UPDATE_INTERVAL = (60000L * 60L * 24L)
    }
}