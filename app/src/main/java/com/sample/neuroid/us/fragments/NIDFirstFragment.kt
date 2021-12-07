package com.sample.neuroid.us.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.models.NIDEventModel
import com.sample.neuroid.us.constants.NID_GO_TO_SECOND_FRAG
import com.sample.neuroid.us.databinding.NidFragmentOneBinding
import com.sample.neuroid.us.interfaces.NIDNavigateFragsListener

class NIDFirstFragment: Fragment() {
    private lateinit var binding : NidFragmentOneBinding
    private var listener: NIDNavigateFragsListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? NIDNavigateFragsListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NidFragmentOneBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            buttonContinueFragOne.setOnClickListener {
                listener?.goToNextScreen(NID_GO_TO_SECOND_FRAG)
            }
            buttonCustomEvent.setOnClickListener {
                val event = NIDEventModel("CUSTOM_EVENT", "button", ts = System.currentTimeMillis())
                //NeuroID.getInstance().captureEvent(event.getOwnJson())
            }
        }
    }
}