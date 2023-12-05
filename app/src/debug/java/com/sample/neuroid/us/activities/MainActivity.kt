package com.sample.neuroid.us.activities

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.R
import com.sample.neuroid.us.activities.sandbox.SandBoxActivity
import com.sample.neuroid.us.databinding.NidActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: NidActivityMainBinding

    lateinit var intentFilter: IntentFilter
    lateinit var receiverNIDCallActivityListener: NIDCallActivityListener

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

            if(checkSelfPermission(
                Manifest.permission.READ_PHONE_STATE
            )== PackageManager.PERMISSION_GRANTED ){
                NIDLog.d("NeuroID call activity", "initializing receiver1")
            intentFilter = IntentFilter("android.intent.action.PHONE_STATE")
            receiverNIDCallActivityListener = NIDCallActivityListener()
            registerReceiver(receiverNIDCallActivityListener,intentFilter)
        }else{

            requestPermissions( arrayOf(android.Manifest.permission.READ_PHONE_STATE),1)

        }
        }
    }
}