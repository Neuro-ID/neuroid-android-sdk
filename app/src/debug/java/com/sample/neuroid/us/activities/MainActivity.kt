package com.sample.neuroid.us.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
    private val READ_PHONE_STATE_REQUEST_CODE = 100

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
            if(!isCallActivityPermissionGiven()){
             requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), READ_PHONE_STATE_REQUEST_CODE)
            }

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
        when(requestCode){
            READ_PHONE_STATE_REQUEST_CODE ->{
                NIDLog.d(msg = "call activity permission granted}")
            }
        }
    }
}