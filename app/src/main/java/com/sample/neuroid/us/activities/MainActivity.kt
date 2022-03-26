package com.sample.neuroid.us.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.databinding.DataBindingUtil
import com.sample.neuroid.us.R
import com.sample.neuroid.us.databinding.NidActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: NidActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.nid_activity_main)

        binding.apply {
            buttonShowActivityNoAutomaticEvents.setOnClickListener {
                startActivity(Intent(this@MainActivity, NIDCustomEventsActivity::class.java))
            }
            buttonShowActivityOneFragment.setOnClickListener {
                startActivity(Intent(this@MainActivity, NIDOnlyOneFragActivity::class.java))
            }
            buttonShowActivityFragments.setOnClickListener {
                startActivity(Intent(this@MainActivity, NIDSomeFragmentsActivity::class.java))
            }
        }

        binding.editTextNormalField.addTextChangedListener( object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                println("----------------- beforeTextChanged")
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                println("----------------- onTextChanged")
            }

            override fun afterTextChanged(p0: Editable?) {
                println("----------------- afterTextChanged")
            }

        })
    }
}