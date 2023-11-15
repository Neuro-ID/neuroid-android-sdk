package com.sample.neuroid.us.activities.sandbox

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sample.neuroid.us.R
import com.sample.neuroid.us.databinding.SandboxActivityBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SandBoxActivity : AppCompatActivity() {

    private val viewModel: SandBoxViewModel by viewModels()

    lateinit var binding: SandboxActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.sandbox_activity)
    }


}