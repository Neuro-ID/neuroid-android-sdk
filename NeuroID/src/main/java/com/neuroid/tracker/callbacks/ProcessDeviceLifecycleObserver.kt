package com.neuroid.tracker.callbacks

import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.events.LOG
import com.neuroid.tracker.service.NIDJobServiceManager
import com.neuroid.tracker.service.NIDNetworkListener
import com.neuroid.tracker.service.getSendingService
import com.neuroid.tracker.utils.NIDMetaData
import kotlinx.coroutines.Dispatchers

/**
 * This is registered against [androidx.lifecycle.ProcessLifecycleOwner], **not** a per-Activity
 * `Application.ActivityLifecycleCallbacks`. `ProcessLifecycleOwner`'s `onStart` only fires when
 * the first `Activity` in the process reaches `onStart()` - i.e. true foreground entry - so it
 * will *not* fire during headless process wake-ups (e.g. a `BroadcastReceiver`-triggered FCM/push
 * cold start) where no `Activity` is ever created.
 */
internal class ProcessDeviceLifecycleObserver(
    private val neuroID: NeuroID,
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        val timeStart = System.currentTimeMillis()

        neuroID.application?.let { app ->
            neuroID.nidJobServiceManager =
                NIDJobServiceManager(
                    neuroID,
                    neuroID.dataStore,
                    getSendingService(
                        neuroID.httpService,
                        app,
                    ),
                    neuroID.logger,
                    neuroID.configService,
                )
        }
        neuroID.logger.i("kurt_test", "lifecycleObserver NIDJobServiceManager: ${System.currentTimeMillis() - timeStart}")

        neuroID.captureApplicationMetaData()
        neuroID.logger.i("kurt_test", "lifecycleObserver captureApplicationMetaData: ${System.currentTimeMillis() - timeStart}")

        if (neuroID.isAdvancedDevice) {
            neuroID.checkThenCaptureAdvancedDevice()
        }
        neuroID.logger.i("kurt_test", "lifecycleObserver checkThenCaptureAdvancedDevice: ${System.currentTimeMillis() - timeStart}")
        neuroID.application?.let { app ->
            neuroID.nidJobServiceManager.startJob(
                app,
                neuroID.clientKey,
            )
            neuroID.logger.i("kurt_test", "lifecycleObserver nidJobServiceManager: ${System.currentTimeMillis() - timeStart}")
            neuroID.metaData =
                NIDMetaData(
                    app.applicationContext
                )
            neuroID.logger.i("kurt_test", "lifecycleObserver NIDMetaData: ${System.currentTimeMillis() - timeStart}")
            app.registerReceiver(
                NIDNetworkListener(
                    neuroID.connectivityManager,
                    neuroID,
                    Dispatchers.IO,
                    neuroID.state,
                ),
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
            )
            neuroID.configService.retrieveOrRefreshCache(neuroID)
        }
        neuroID.logger.i("kurt_test", "lifecycleObserver retrieveOrRefreshCache: ${System.currentTimeMillis() - timeStart}")
        neuroID.resetClientId()
        neuroID.captureEvent(queuedEvent = true, type = LOG, m = "isAdvancedDevice setting $neuroID.isAdvancedDevice", level = "INFO")
        neuroID.logger.i("kurt_test", "lifecycleObserver resetClientId (end): ${System.currentTimeMillis() - timeStart}")
    }
}
