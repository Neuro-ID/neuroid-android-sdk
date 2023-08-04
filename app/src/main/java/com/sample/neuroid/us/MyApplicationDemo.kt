package com.sample.neuroid.us

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.multidex.MultiDexApplication
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.extensions.setVerifyIntegrationHealth
import com.sample.neuroid.us.domain.config.ConfigHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplicationDemo : MultiDexApplication() {

    @Inject
    lateinit var configHelper: ConfigHelper

    override fun onCreate() {
        super.onCreate()
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
        val neuroId = NeuroID.Builder(
            this,
            "key_live_suj4CX90v0un2k1ufGrbItT5"
        ).build()
        NeuroID.setNeuroIdInstance(neuroId)
        NeuroID.getInstance()?.setEnvironmentProduction(true)
        NeuroID.getInstance()?.setSiteId(configHelper.formId)
        NeuroID.getInstance()?.setVerifyIntegrationHealth(true)

        NeuroID.getInstance()?.start()
        NeuroID.getInstance()?.setUserID(configHelper.userId)
    }
}