package com.neuroid.tracker.events

import android.os.Looper

// run the function after a 300ms delay in react native
internal fun handleIdentifyAllViews(r:Runnable){
    android.os.Handler(Looper.getMainLooper()).postDelayed({
        r.run()
    }, 300)
}