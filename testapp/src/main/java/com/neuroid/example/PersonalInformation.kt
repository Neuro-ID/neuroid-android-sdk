package com.neuroid.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.datepicker.MaterialDatePicker
import com.neuroid.example.databinding.PersonalInformationBinding
import com.neuroid.tracker.NeuroID

class PersonalInformation : LayoutActivity() {
    private lateinit var binding: PersonalInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NeuroID.getInstance()?.setScreenName(this.localClassName)

        binding = PersonalInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.header.neuroidTitle.setOnClickListener(
            showNIDDataOnClickListener(this.localClassName),
        )

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Birthdate")
                .build()
        datePicker.addOnPositiveButtonClickListener {
            binding.dateofbirth.setText(datePicker.headerText)
        }
        binding.dateofbirth.setOnClickListener {
            datePicker.show(supportFragmentManager, "test")
        }

        val home = Intent(this, Instructions::class.java)
        val next = Intent(this, LoanApplication::class.java)
        val previous = Intent(this, Instructions::class.java)

        binding.header.pageLabel.text = getString(R.string.personal_information)
        binding.header.previous.setOnClickListener {
            updateData()
            startActivity(previous)
        }
        binding.footer.home.setOnClickListener {
            updateData()

            (application as ApplicationMain).stopTracking()
            startActivity(home)
        }

        binding.footer.next.setOnClickListener {
            binding.firstname.text?.let {
                if (it.isEmpty()) {
                    Toast.makeText(this, "Firstname cannot be blank", Toast.LENGTH_LONG).show()
                } else {
                    updateData()
                    startActivity(next)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.firstname.setText(ApplicationMain.dataManager.allInfo.firstName)
        binding.lastname.setText(ApplicationMain.dataManager.allInfo.lastName)
        binding.dateofbirth.setText(ApplicationMain.dataManager.allInfo.dob)
    }

    fun updateData() {
        ApplicationMain.dataManager.allInfo.firstName = binding.firstname.text.toString()
        ApplicationMain.dataManager.allInfo.lastName = binding.lastname.text.toString()
        ApplicationMain.dataManager.allInfo.dob = binding.dateofbirth.text.toString()
    }
}
