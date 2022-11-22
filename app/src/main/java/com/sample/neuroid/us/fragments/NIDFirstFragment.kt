package com.sample.neuroid.us.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.sample.neuroid.us.R
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

            val listColors = resources.getStringArray(R.array.nid_array_colors)
            val adapterColors = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listColors
            )
            autoCompleteTextColor.apply {
                setAdapter(adapterColors)
                setOnItemClickListener { _, _, _, _ ->
                    println("--------------------- onItemClickListener Example")
                }
            }

            editTextNormalField.addTextChangedListener(object : TextWatcher {
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
}