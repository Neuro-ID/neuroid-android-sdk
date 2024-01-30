package com.sample.neuroid.us.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.neuroid.tracker.NeuroID
import com.sample.neuroid.us.R
import com.sample.neuroid.us.activities.sandbox.SandBoxActivity
import com.sample.neuroid.us.databinding.NidActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: NidActivityMainBinding

    val LOCATION_REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                NeuroID.getInstance()?.closeSession()
            }
        }
        if (!isLocationPermissionGiven()) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        } else {
            // since permission is given after SDK start, we need to
            // resync the location
            NeuroID.getInstance()?.syncLocation()
        }
    }

    private fun isLocationPermissionGiven(): Boolean {
        val coarse = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return coarse && fine
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            LOCATION_REQUEST_CODE -> {
                // since permission is given after SDK start, we need to
                // resync the location
                NeuroID.getInstance()?.syncLocation()
            }
        }
    }
}