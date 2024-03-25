package com.sample.neuroid.us.utils

import android.location.Location
import android.location.LocationListener
import com.google.gson.InstanceCreator
import java.lang.reflect.Type

/**
 * GSON needs for parsing objects during testing. No used in app.
 */
class LocationListenerCreator: InstanceCreator<LocationListener> {
    override fun createInstance(type: Type?): LocationListener {
       return object: LocationListener{
           override fun onLocationChanged(p0: Location) {
               println("do nothing")
           }
       }
    }

}