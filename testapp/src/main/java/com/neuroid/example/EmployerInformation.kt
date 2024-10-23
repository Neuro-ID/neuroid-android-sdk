package com.neuroid.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.neuroid.example.databinding.EmployerInformationBinding
import com.neuroid.tracker.NeuroID

class EmployerInformation : LayoutActivity() {
    private lateinit var binding: EmployerInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EmployerInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        NeuroID.getInstance()?.setScreenName(this.localClassName)
        binding.header.neuroidTitle.setOnClickListener(
            showNIDDataOnClickListener(this.localClassName),
        )
        val home = Intent(this, Instructions::class.java)
        val next = Intent(this, ApplicationSummary::class.java)
        val previous = Intent(this, AddressInformation::class.java)
        binding.header.pageLabel.text = getString(R.string.employer_information)
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
            updateData()
            startActivity(next)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.employerName.setText(ApplicationMain.dataManager.allInfo.employerName)
        binding.employerAddress.setText(ApplicationMain.dataManager.allInfo.employerAddress)
        binding.employerPhone.setText(ApplicationMain.dataManager.allInfo.employerPhone)
    }

    fun updateData() {
        ApplicationMain.dataManager.allInfo.employerName = binding.employerName.text.toString()
        ApplicationMain.dataManager.allInfo.employerAddress = binding.employerAddress.text.toString()
        ApplicationMain.dataManager.allInfo.employerPhone = binding.employerPhone.text.toString()
    }
}
