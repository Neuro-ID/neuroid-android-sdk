package com.neuroid.example

import android.content.Intent
import android.os.Bundle
import com.google.android.material.datepicker.MaterialDatePicker
import com.neuroid.example.databinding.PersonalInformationTwoBinding
import com.neuroid.tracker.NeuroID

class PersonalInformationTwo: LayoutActivity() {
    private lateinit var binding: PersonalInformationTwoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NeuroID.getInstance()?.setScreenName(this.localClassName)
        binding = PersonalInformationTwoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Birthdate")
                .build()
        datePicker.addOnPositiveButtonClickListener {
            binding.dateofbirth.setText(datePicker.headerText)
            updateData()
        }
        binding.dateofbirth.setOnClickListener {
            datePicker.show(supportFragmentManager, "test")
        }
        binding.header.previous.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        binding.header.pageLabel.text = getString(R.string.personal_information)

        binding.signupButton.setOnClickListener{
            startActivity(Intent(this, Instructions::class.java))
        }
    }

    private fun updateData() {
        ApplicationMain.dataManager.allInfo.dob = binding.dateofbirth.text.toString()
    }
}