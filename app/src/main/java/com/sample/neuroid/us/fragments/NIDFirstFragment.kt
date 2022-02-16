package com.sample.neuroid.us.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sample.neuroid.us.constants.NID_GO_TO_SECOND_FRAG
import com.sample.neuroid.us.databinding.NidFragmentOneBinding
import com.sample.neuroid.us.interfaces.NIDNavigateFragsListener
import java.util.*

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
        val calendar = Calendar.getInstance()

        binding.apply {
            buttonContinueFragOne.setOnClickListener {
                listener?.goToNextScreen(NID_GO_TO_SECOND_FRAG)
            }

            buttonShowDatePicker.setOnClickListener {
                val datePicker = DatePickerDialog(
                    requireContext(),
                    { _, year, month, day ->
                        val date = "$day/${month + 1}/$year"
                        editTextDate.setText(date)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                datePicker.show()
            }

            buttonShowTimePicker.setOnClickListener {
                val timePicker = TimePickerDialog(
                    requireContext(),
                    {  _, hour, minute ->
                        val time = "$hour:$minute"
                        editTextTime.setText(time)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(requireContext())
                )

                timePicker.show()
            }
        }
    }
}