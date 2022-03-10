package com.sample.neuroid.us.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sample.neuroid.us.databinding.NidFragmentOnlyOneBinding

class NIDOnlyOneFragment: Fragment() {
    private lateinit var binding : NidFragmentOnlyOneBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NidFragmentOnlyOneBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.seekBarOne.max = 100
        binding.seekBarOne.progress = 20
        //simulateService()
    }

    private fun simulateService() {
        Handler(Looper.getMainLooper()).postDelayed({
            changeMax()
        }, 4000) //Simulate time to response service call
    }

    private fun changeMax() {
        binding.seekBarOne.max = 50
    }
}