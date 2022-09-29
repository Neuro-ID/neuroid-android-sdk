package com.sample.neuroid.us.activities.sandbox

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.neuroid.tracker.NeuroID
import com.sample.neuroid.us.R
import com.sample.neuroid.us.databinding.SandboxActivityBinding

class SandBoxActivity : AppCompatActivity() {

    var binding: SandboxActivityBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NeuroID.getInstance()?.excludeViewByResourceID("textView_sid_value")

        binding = DataBindingUtil.setContentView(this, R.layout.sandbox_activity)
        binding?.apply {
            NeuroID.getInstance()?.setScreenName("PERSONAL_DETAILS")

            val yearList = resources.getStringArray(R.array.nid_app_array_years)
            val monthList = resources.getStringArray(R.array.nid_app_array_months)

            val adapterYear = ArrayAdapter(
                applicationContext,
                android.R.layout.simple_dropdown_item_1line,
                yearList
            )

            editTextBirthYear.apply {
                setAdapter(adapterYear)
            }

            val adapterMonth = ArrayAdapter(
                applicationContext,
                android.R.layout.simple_dropdown_item_1line,
                monthList
            )

            dobMonth.apply {
                setAdapter(adapterMonth)
                setOnItemClickListener { _, _, position, _ ->
                    editTextBirthDay.setText("")
                    editTextBirthDay.isEnabled = true
                    editTextBirthDay.setAdapter(getAdapter(position))
                }
            }

            buttonContinue.setOnClickListener {
                finish()
            }

        }
    }

    private fun getAdapter(position: Int): ArrayAdapter<Int> {
        return ArrayAdapter(
            applicationContext,
            android.R.layout.simple_dropdown_item_1line,
            getDays(position)
        )
    }

    private fun getDays(position: Int): List<Int> {
        return when (position) {
            0, 2, 4, 6, 7, 9, 11 -> (1..31).toList()
            3, 5, 8, 10 -> (1..30).toList()
            else -> (1..28).toList()
        }
    }
}