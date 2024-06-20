package com.neuroid.tracker.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.core.location.LocationListenerCompat
import com.neuroid.tracker.models.NIDLocation
import com.neuroid.tracker.utils.LocationPermissionUtils
import com.neuroid.tracker.utils.NIDMetaData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Service to get the last known location and to request the current location.
 *
 * Start a request to get a location for a provider. Once the location is obtained, update
 * the location to a passed in NIDLocation instance. If multiple providers are found, choose
 * one using the PROVIDER_MAP. Highest number is the most desired (most accurate) provider.
 */
class LocationService(private val locationPermissionUtils: LocationPermissionUtils = LocationPermissionUtils()) {
    private var nidLocation: NIDLocation? = null
    private var isStarted = false
    private var locationScope: CoroutineScope? = null

    @SuppressLint("MissingPermission")
    private val locationListener = LocationListenerCompat { location ->
        nidLocation?.longitude = location.longitude
        nidLocation?.latitude = location.latitude
        nidLocation?.authorizationStatus = NIDMetaData.LOCATION_AUTHORIZED_ALWAYS
    }

    /**
     * this will setup a new coroutine for use in requestLocation(). requestLocation() requries a
     * looper. tied to the session lifecycle, called in NeuroID on start, startSession, resumeCollection,
     */
    fun setupLocationCoroutine(locationManager: LocationManager) {
        if (isStarted) {
            shutdownLocationCoroutine(locationManager)
        }
        if (locationScope == null || locationScope?.isActive == false) {
            // this will allow us to get the looper
            var handlerThread = HandlerThread("location_service_thread")
            handlerThread.start()
            // get the looper from the thread
            val looper = handlerThread.looper
            // turn it into a dispatcher with a Handler that we can use in a coroutine
            val locationDispatcher = Handler(looper).asCoroutineDispatcher()
            // setup the coroutine scope with the looper and Handler that we can use
            locationScope = CoroutineScope(locationDispatcher)
        }
    }

    /**
     * this will cancel the coroutine scope used by requestLocation() to request locations.
     * lcoation requesting will stop when listener is removend and locationScope is canceled.
     * tied to the session lifecycle, called in NeuroID on stop, pauseCollection, stopSession
     */
    @SuppressLint("MissingPermission")
    fun shutdownLocationCoroutine(locationManager: LocationManager) {
        locationManager.removeUpdates(locationListener)
        locationScope?.cancel()
        isStarted = false
    }

    fun isLocationServiceListening(): Boolean = isStarted

    /**
     * called when we get metadata to get the last known position pulled from the location
     * requestor. coroutine param is for testing purposes. normal operation will use the
     * internal coroutine scope.
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(
        context: Context,
        nidLocation: NIDLocation,
        scope: CoroutineScope? = locationScope,
        locationManager: LocationManager,
        isLocationAllowed: Boolean,
    ) {
        if (locationPermissionUtils.isNotAllowedToCollectLocations(context) || !isLocationAllowed) {
            shutdownLocationCoroutine(locationManager)
            return
        }
        this.nidLocation = nidLocation
        // get last position if available, take highest accuracy from available providers
        var smallestAccuracyMeters = Float.MAX_VALUE
        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                if (location.accuracy < smallestAccuracyMeters) {
                    this.nidLocation?.let {
                        it.longitude = location.longitude
                        it.latitude = location.latitude
                        it.authorizationStatus =
                            NIDMetaData.LOCATION_AUTHORIZED_ALWAYS
                    }
                    smallestAccuracyMeters = location.accuracy
                }
            }
        }
        requestLocation(context, scope, locationManager)
    }

    /**
     * start requesting location. locations will be polled based on the time and distance from the
     * last collected location.
     */
    @SuppressLint("MissingPermission")
    private fun requestLocation(
        context: Context,
        scope: CoroutineScope?,
        locationManager: LocationManager,
    ) {
        if (!isStarted) {
            if (locationPermissionUtils.isNotAllowedToCollectLocations(context)) {
                shutdownLocationCoroutine(locationManager)
                return
            }
            // request a location
            // choose the most accurate provider for the job
            val provider = getBestProvider(locationManager)
            if (provider != "") {
                // location scope coroutine is setup in NeuroID and tied to the
                // session lifecycle.
                scope?.let { scope ->
                    scope.launch {
                        isStarted = true
                        // done this way because requestLocationUpdates() runs in a looper to get
                        // location data
                        locationManager.requestLocationUpdates(
                            provider,
                            MIN_TIME_INTERVAL,
                            MIN_DISTANCE_INTERVAL,
                            locationListener,
                            Looper.myLooper(),
                        )
                        Looper.loop()
                        isStarted = true
                    }
                }
            }
        }
    }

    private fun getBestProvider(locationManager: LocationManager): String {
        var bestProvider = ""
        var bestProviderScore = 0
        val providers = locationManager.getProviders(true)
        providers.forEach { provider ->
            val score = PROVIDER_MAP[provider]
            score?.let {
                if (it > bestProviderScore) {
                    bestProviderScore = score
                    bestProvider = provider
                }
            }
        }
        return bestProvider
    }

    companion object {
        private val PROVIDER_MAP =
            mapOf(
                LocationManager.GPS_PROVIDER to 3,
                LocationManager.PASSIVE_PROVIDER to 2,
                LocationManager.NETWORK_PROVIDER to 1,
            )
        const val MIN_TIME_INTERVAL = 10000L // milliseconds
        const val MIN_DISTANCE_INTERVAL = 10F // meters
    }
}
