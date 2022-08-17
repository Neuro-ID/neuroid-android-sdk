package com.sample.neuroid.us.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.neuroid.tracker.NeuroID
import com.sample.neuroid.us.R
import com.sample.neuroid.us.databinding.NidActivityCustomEventsBinding

class NIDCustomEventsActivity : AppCompatActivity() {
    private lateinit var binding: NidActivityCustomEventsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.nid_custom_events_title_activity)
        binding = DataBindingUtil.setContentView(this, R.layout.nid_activity_custom_events)
        NeuroID.getInstance()?.setScreenName("NID_CUSTOM_EVENT_PAGE")

        binding.apply {
            buttonSendCustomEvent.setOnClickListener {
                NeuroID.getInstance()?.captureEvent(
                    eventName = "CUSTOM_EVENT",
                    tgs = "buttonSendCustomEvent"
                )
                showToast()
            }
            buttonSendFormSubmit.setOnClickListener {
                NeuroID.getInstance()?.formSubmit()
                showToast()
            }
            buttonSendFormSuccess.setOnClickListener {
                NeuroID.getInstance()?.formSubmitSuccess()
                showToast()
            }
            buttonSendFormFailure.setOnClickListener {
                NeuroID.getInstance()?.formSubmitFailure()
                showToast()
            }
        }
    }

    private fun showToast() {
        Toast.makeText(
            this@NIDCustomEventsActivity,
            R.string.nid_custom_events_saved_event_activity,
            Toast.LENGTH_LONG
        ).show()
    }
}