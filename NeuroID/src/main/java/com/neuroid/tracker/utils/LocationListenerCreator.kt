package com.neuroid.tracker.utils

import android.location.Location
import android.location.LocationListener
import com.google.gson.InstanceCreator
import java.lang.reflect.Type

class LocationListenerCreator: InstanceCreator<LocationListener> {
    override fun createInstance(type: Type?): LocationListener {
       return object: LocationListener{
           override fun onLocationChanged(p0: Location) {
               println("do nothing")
           }
       }
    }

}