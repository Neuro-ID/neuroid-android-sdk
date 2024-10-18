package com.neuroid.tracker.service

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import android.location.LocationManager.PASSIVE_PROVIDER
import android.os.Looper
import com.neuroid.tracker.models.NIDLocation
import com.neuroid.tracker.utils.LocationPermissionUtils
import com.neuroid.tracker.utils.NIDMetaData.Companion.LOCATION_AUTHORIZED_ALWAYS
import com.neuroid.tracker.utils.NIDMetaData.Companion.LOCATION_UNKNOWN
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NIDLocationServiceTest {
    @Test
    fun locationServiceShutdownLocationCoroutine() {
        val locationPermissionUtils = mockk<LocationPermissionUtils>()
        every { locationPermissionUtils.isNotAllowedToCollectLocations(any()) } returns false
        val locationManager = mockk<LocationManager>()
        every { locationManager.removeUpdates(any<LocationListener>()) } just runs

        val locationService = LocationService(locationPermissionUtils)
        locationService.shutdownLocationCoroutine(locationManager)
        assertFalse(locationService.isLocationServiceListening())
    }

    @Test
    fun locationServiceSetupLocationCoroutine() {
        val locationPermissionUtils = mockk<LocationPermissionUtils>()
        every { locationPermissionUtils.isNotAllowedToCollectLocations(any()) } returns false
        val locationManager = mockk<LocationManager>()
        every { locationManager.removeUpdates(any<LocationListener>()) } just runs

        val locationService = LocationService(locationPermissionUtils)
        locationService.setupLocationCoroutine(locationManager)
        assertFalse(locationService.isLocationServiceListening())
    }

    @Test
    fun getLastKnownLocation_isStarted() {
        val locationPermissionUtils = mockk<LocationPermissionUtils>()
        every { locationPermissionUtils.isNotAllowedToCollectLocations(any()) } returns false
        val context = mockk<Context>()
        val androidLocation = mockk<Location>()
        every { androidLocation.latitude } returns 2.0
        every { androidLocation.longitude } returns 3.0
        every { androidLocation.provider } returns GPS_PROVIDER
        every { androidLocation.accuracy } returns 1F
        val locationManager = mockk<LocationManager>()
        every { locationManager.getLastKnownLocation(any()) } returns androidLocation
        every { locationManager.getProviders(true) } returns
            listOf(
                GPS_PROVIDER,
                PASSIVE_PROVIDER,
                NETWORK_PROVIDER,
            )
        every { locationManager.removeUpdates(any<LocationListener>()) } just runs
        every { locationManager.requestLocationUpdates(any<String>(), any(), any(), any(), any<Looper>()) } just runs

        val locationService = LocationService(locationPermissionUtils)
        val location = NIDLocation(-1.0, -1.0, LOCATION_UNKNOWN)
        locationService.getLastKnownLocation(context, location, CoroutineScope(Dispatchers.Unconfined), locationManager, true)
        assertEquals(3.0, location.longitude, 0.0)
        assertEquals(2.0, location.latitude, 0.0)
        assertEquals(location.authorizationStatus, LOCATION_AUTHORIZED_ALWAYS)
        verify { locationManager.requestLocationUpdates(any<String>(), any(), any(), any(), any<Looper>()) }
        assertTrue(locationService.isLocationServiceListening())
    }

    @Test
    fun getLastKnownLocation_isNotStarted() {
        val locationPermissionUtils = mockk<LocationPermissionUtils>()
        every { locationPermissionUtils.isNotAllowedToCollectLocations(any()) } returns false
        val context = mockk<Context>()
        val androidLocation = mockk<Location>()
        every { androidLocation.latitude } returns 2.0
        every { androidLocation.longitude } returns 3.0
        every { androidLocation.provider } returns GPS_PROVIDER
        every { androidLocation.accuracy } returns 1F
        val locationManager = mockk<LocationManager>()
        every { locationManager.getLastKnownLocation(any()) } returns androidLocation
        every { locationManager.getProviders(true) } returns
            listOf(
                GPS_PROVIDER,
                PASSIVE_PROVIDER,
                NETWORK_PROVIDER,
            )
        every { locationManager.removeUpdates(any<LocationListener>()) } just runs
        every { locationManager.requestLocationUpdates(any<String>(), any(), any(), any(), any<Looper>()) } just runs

        val locationService = LocationService(locationPermissionUtils)
        val location = NIDLocation(-1.0, -1.0, LOCATION_UNKNOWN)
        locationService.getLastKnownLocation(context, location, CoroutineScope(Dispatchers.Unconfined), locationManager, false)
        verify(exactly = 0) { locationManager.getLastKnownLocation(any()) }
        verify { locationManager.removeUpdates(any<LocationListener>()) }
    }
}
