package com.neuroid.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.neuroid.example.databinding.PersonalInformationTwoBinding
import com.neuroid.tracker.NeuroID

class PersonalInformationTwoFragment: Fragment() {

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        NeuroID.getInstance()?.setScreenName((activity as SignUp).localClassName)
        val personalInformationTwoBinding = PersonalInformationTwoBinding.inflate(layoutInflater, container, false)
        setupUiWithBinding(personalInformationTwoBinding)
        NeuroID.getInstance()?.stop()
        return personalInformationTwoBinding.root
    }

    fun setupUiWithBinding(personalInformationTwoBinding: PersonalInformationTwoBinding) {
        val headerText = personalInformationTwoBinding.header.pageLabel
        headerText.text = "Personal Information 2"
        val signup = personalInformationTwoBinding.signupButton
        val previous = personalInformationTwoBinding.header.previous
        val dataOfBirth = personalInformationTwoBinding.dateofbirth
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Birthdate")
                .build()
        datePicker.addOnPositiveButtonClickListener {
            dataOfBirth.setText(datePicker.headerText)
            (activity as SignUp).updateDOB(dataOfBirth.text.toString())
        }
        dataOfBirth.setOnClickListener {
            activity?.supportFragmentManager?.let {
                datePicker.show(it, "test")
            }
        }

        signup.setOnClickListener {
            (activity as SignUp).onContinueButtonSecond("test this")
        }
        previous.setOnClickListener {
            (activity as SignUp).onPreviousPersonalInfoTwo()
        }
    }
}