package com.neuroid.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.*
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.neuroid.example.databinding.SplashBinding
import com.neuroid.tracker.NeuroID

private val REQUEST_CODE = 1

class Splash: LayoutActivity() {
    private lateinit var binding: SplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //reset splash activity state back to starting state so test runner can rerun test
        (application as ApplicationMain).resetAll()

        NeuroID.getInstance()?.setScreenName(this.localClassName)
        NeuroID.getInstance()?.setRegisteredUserID("${ApplicationMain.registeredUserName}${ApplicationMain.registeredUserID}")
        ApplicationMain.registeredUserID = ApplicationMain.registeredUserID + 1
        binding = SplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (ApplicationMain.isLoggedIn) {
            binding.login.isEnabled = false
            binding.login.alpha = 0.5f
        }
        binding.login.setOnClickListener {
            NeuroID.getInstance()?.startAppFlow("form_parks912", "${ApplicationMain.sessionName}${ApplicationMain.registeredSessionId}")
            startActivity(Intent(this, Login::class.java))
        }
        binding.signup.setOnClickListener {
            NeuroID.getInstance()?.startAppFlow("form_skein469", "${ApplicationMain.sessionName}${ApplicationMain.registeredSessionId}")
            startActivity(Intent(this, SignUp::class.java))
        }
        val permissionsRequests = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsRequests.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsRequests.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsRequests.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (permissionsRequests.isNotEmpty()) {
            if (VERSION.SDK_INT > 22) {
                requestPermissions(permissionsRequests.toTypedArray(), REQUEST_CODE)
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE -> {
                for (i in 0..permissions.size - 1) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        // Permission was granted
                    } else {
                        // Permission denied
                        Toast.makeText(
                            this,
                            "${permissions[i]} permission denied",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            else -> {
                // Ignore all other requests
            }
        }
    }

}