package com.sample.neuroid.us.activities

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.R
import com.sample.neuroid.us.activities.sandbox.SandBoxActivity
import com.sample.neuroid.us.databinding.NidActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: NidActivityMainBinding
    private val REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // NeuroID.getInstance()?.startSession("gasdgdasg")
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