package com.sample.neuroid.us.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sample.neuroid.us.constants.NID_GO_TO_THIRD_FRAG
import com.sample.neuroid.us.databinding.NidFragmentTwoBinding
import com.sample.neuroid.us.interfaces.NIDNavigateFragsListener

class NIDSecondFragment: Fragment() {
    private lateinit var binding : NidFragmentTwoBinding
    private var listener: NIDNavigateFragsListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? NIDNavigateFragsListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NidFragmentTwoBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            buttonContinueFragTwo.setOnClickListener {
                listener?.goToNextScreen(NID_GO_TO_THIRD_FRAG)
            }
        }
    }
}