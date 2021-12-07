package com.sample.neuroid.us.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sample.neuroid.us.constants.NID_FINISH_ACTIVITY
import com.sample.neuroid.us.databinding.NidFragmentThreeBinding
import com.sample.neuroid.us.interfaces.NIDNavigateFragsListener

class NIDThirdFragment: Fragment() {
    private lateinit var binding : NidFragmentThreeBinding
    private var listener: NIDNavigateFragsListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? NIDNavigateFragsListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NidFragmentThreeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            buttonFinish.setOnClickListener {
                listener?.goToNextScreen(NID_FINISH_ACTIVITY)
            }
        }
    }
}