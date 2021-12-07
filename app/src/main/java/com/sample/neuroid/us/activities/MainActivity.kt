package com.sample.neuroid.us.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.sample.neuroid.us.R
import com.sample.neuroid.us.databinding.NidActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: NidActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.nid_activity_main)

        binding.apply {
            buttonShowActivityOneFragment.setOnClickListener {
                startActivity(Intent(this@MainActivity, NIDOnlyOneFragActivity::class.java))
            }
            buttonShowActivityFragments.setOnClickListener {
                startActivity(Intent(this@MainActivity, NIDSomeFragmentsActivity::class.java))
            }
        }
    }
}