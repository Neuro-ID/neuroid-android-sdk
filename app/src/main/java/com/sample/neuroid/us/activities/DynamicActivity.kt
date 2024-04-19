package com.sample.neuroid.us.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.utils.NIDLog
import com.sample.neuroid.us.R
import com.sample.neuroid.us.databinding.NidActivityDynamicBinding


class DynamicActivity : AppCompatActivity() {
    private lateinit var binding: NidActivityDynamicBinding
    private lateinit var editText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.nid_activity_dynamic
        )
        binding.apply {
            btnAdd.setOnClickListener {
                btnAddWithRegisterTarget.visibility = View.VISIBLE
                addNewView(llContainer)
            }
            btnAddWithRegisterTarget.setOnClickListener {
                addNewViewWithRegisterTarget()
            }
        }
    }

    private fun addNewView(viewGroup: ViewGroup) {
        editText = EditText(this)
        editText.tag = "etNewEditText"
        editText.setText("New EditText")
        viewGroup.addView(editText)
    }

    private fun addNewViewWithRegisterTarget() {
        // re-register targets after we add an EditText to the layout.
        NeuroID.getInstance()?.registerPageTargets(this)
    }
}