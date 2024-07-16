package com.sample.neuroid.us.activities

import android.Manifest
import android.app.ActivityManager
import android.app.Application
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Debug
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.R
import com.sample.neuroid.us.activities.sandbox.SandBoxActivity
import com.sample.neuroid.us.databinding.NidActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private lateinit var binding: NidActivityMainBinding
    private val REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val memEater = MemEater(this)

        NeuroID.getInstance()?.startSession("gasdgdasg")
        // ensure that the phone stays on for the duration of the test
        println("MainActivity: isStopped() ${NeuroID.getInstance()?.isStopped()}")
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val keyguardLock = km.newKeyguardLock("TAG")
        keyguardLock.disableKeyguard()
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        binding = DataBindingUtil.setContentView(this, R.layout.nid_activity_main)

        binding.apply {
            textViewSidValue.setText(NeuroID.getInstance()?.getSessionId())
            textViewCidValue.setText(NeuroID.getInstance()?.getClientId())
            buttonShowActivityNoAutomaticEvents.setOnClickListener {
                startActivity(Intent(this@MainActivity, NIDCustomEventsActivity::class.java))
            }
            buttonShowActivityOneFragment.setOnClickListener {
                startActivity(Intent(this@MainActivity, NIDOnlyOneFragActivity::class.java))
            }
            buttonShowActivityFragments.setOnClickListener {
                startActivity(Intent(this@MainActivity, NIDSomeFragmentsActivity::class.java))
            }
            buttonShowSandBox.setOnClickListener {
                startActivity(Intent(this@MainActivity, SandBoxActivity::class.java))
            }
            buttonShowDynamic.setOnClickListener {
                startActivity(Intent(this@MainActivity, DynamicActivity::class.java))
            }
            buttonPayloadJson.setOnClickListener {
                startActivity(Intent(this@MainActivity, NIDPayloadJsonActivity::class.java))
            }
            buttonCloseSession.setOnClickListener {
                NeuroID.getInstance()?.stopSession()
            }
            startMemEater.setOnClickListener {
                memEater.eat()
            }
            stopMemEater.setOnClickListener() {
                memEater.stopEating()
            }
        }

        val permissions = mutableListOf<String>()

        if (!isLocationPermissionGiven()) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (!isCallActivityPermissionGiven()){
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), REQUEST_CODE)
        }
    }

    class MemEater(val context: Context) {
        var buffer = mutableListOf<String>()
        var isRunning = false
        var job: Job? = null

        fun stopEating(){
            job?.let {
                it.cancel()
                buffer.clear()
                buffer = mutableListOf()
                isRunning = false
                println("createResetLowMemoryWatchdogServer canceling eater")
            }
        }
        fun eat() {
            isRunning = true
            println("createResetLowMemoryWatchdogServer starting eater")
            job = CoroutineScope(Dispatchers.IO).launch {
                while(isRunning) {
                    val stringBuffer = StringBuffer()
                    for(i in 0..100000) {
                        stringBuffer.append("h453")
                    }
                    buffer.add(stringBuffer.toString())
                    val mi = ActivityManager.MemoryInfo()
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    activityManager.getMemoryInfo(mi)
                    println("eater: total: ${mi.totalMem} avail: ${mi.availMem} threshold: ${mi.threshold} islow: ${mi.lowMemory}")
                    println("eater: free ${Runtime.getRuntime().freeMemory()} max: ${Runtime.getRuntime().maxMemory()} total: ${Runtime.getRuntime().totalMemory()}")
                    delay(500L)
                }
            }
        }
    }


    private fun isLocationPermissionGiven(): Boolean {
        val coarse = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return coarse && fine
    }

    private fun isCallActivityPermissionGiven(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_CODE -> {
                if (isLocationPermissionGiven()) {
                    Log.d(
                        "main_activity",
                        "location grantResults ok"
                    )
                } else {
                    Log.d(
                        "main_activity",
                        "location grantResults denied"
                    )
                }
                if (isCallActivityPermissionGiven()) {
                    NIDLog.d(msg = "call activity permission granted")
                } else {
                    NIDLog.d(msg = "call activity permission denied")
                }
            }
        }
    }

}