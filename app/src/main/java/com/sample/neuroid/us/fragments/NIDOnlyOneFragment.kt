package com.sample.neuroid.us.fragments

import android.os.Bundle
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
}