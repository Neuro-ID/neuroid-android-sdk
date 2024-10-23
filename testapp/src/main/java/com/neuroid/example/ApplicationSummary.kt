package com.neuroid.example

import android.content.Intent
import android.os.Bundle
import com.neuroid.example.databinding.ApplicationSummaryBinding
import com.neuroid.tracker.NeuroID

class ApplicationSummary : LayoutActivity() {
    private lateinit var binding: ApplicationSummaryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ApplicationSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        NeuroID.getInstance()?.setScreenName(this.localClassName)
        binding.header.neuroidTitle.setOnClickListener(
            showNIDDataOnClickListener(this.localClassName),
        )
        val home = Intent(this, Instructions::class.java)
        val previous = Intent(this, EmployerInformation::class.java)
        binding.header.pageLabel.text = getString(R.string.application_summary)
        binding.header.previous.setOnClickListener {
            startActivity(previous)
        }
        binding.home.setOnClickListener {
            ApplicationMain.isApplicationSubmitted = true
            startActivity(home)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.personalInfoName.text = "${ApplicationMain.dataManager.allInfo.firstName}, ${ApplicationMain.dataManager.allInfo.lastName}"
        binding.personalInfoDob.text = ApplicationMain.dataManager.allInfo.dob
        binding.contactInfoEmail.text = ApplicationMain.dataManager.allInfo.email
        binding.contactInfoPhone.text = ApplicationMain.dataManager.allInfo.phone
        binding.addressInfoAddress.text = ApplicationMain.dataManager.allInfo.address
        binding.addressInfoCity.text = ApplicationMain.dataManager.allInfo.city
        binding.addressInfoZip.text = ApplicationMain.dataManager.allInfo.zip
        binding.employerInfoName.text = ApplicationMain.dataManager.allInfo.employerName
        binding.employerInfoAddress.text = ApplicationMain.dataManager.allInfo.employerAddress
        binding.employerInfoPhone.text = ApplicationMain.dataManager.allInfo.employerPhone
    }
}
