package com.sample.neuroid.us.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.R
import com.sample.neuroid.us.databinding.NidActivityJsonPayloadBinding

class NIDPayloadJsonActivity : AppCompatActivity() {
    private lateinit var binding: NidActivityJsonPayloadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.nid_custom_events_title_activity)
        binding = DataBindingUtil.setContentView(this, R.layout.nid_activity_json_payload)
        NeuroID.getInstance()?.let {
            if (it.isStopped()) {
                it.start()
            }
            it.setScreenName("NID CUSTOM EVENT PAGE")
        }

        binding.apply {
            buttonShowPayloadJson.setOnClickListener {
                NIDLog.d(msg = "Payload json: null")
            }

            buttonResetPayloadJson.setOnClickListener {
                showToast()
            }
        }
    }

    private fun showToast() {
        Toast.makeText(
            this@NIDPayloadJsonActivity,
            "Reset Json",
            Toast.LENGTH_LONG
        ).show()
    }
}