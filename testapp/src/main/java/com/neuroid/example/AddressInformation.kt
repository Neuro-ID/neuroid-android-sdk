package com.neuroid.example

import android.content.Intent
import android.os.Bundle
import com.neuroid.example.databinding.AddressInformationBinding
import com.neuroid.tracker.NeuroID

class AddressInformation : LayoutActivity() {
    private lateinit var binding: AddressInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddressInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        NeuroID.getInstance()?.setScreenName(this.localClassName)
        binding.header.neuroidTitle.setOnClickListener(showNIDDataOnClickListener(this.localClassName))
        val home = Intent(this, Instructions::class.java)
        val next = Intent(this, EmployerInformation::class.java)
        val previous = Intent(this, ContactInformation::class.java)
        binding.header.pageLabel.text = getText(R.string.address_information)
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
        binding.address.setText(ApplicationMain.dataManager.allInfo.address)
        binding.city.setText(ApplicationMain.dataManager.allInfo.city)
        binding.zip.setText(ApplicationMain.dataManager.allInfo.zip)
    }

    fun updateData() {
        ApplicationMain.dataManager.allInfo.address = binding.address.text.toString()
        ApplicationMain.dataManager.allInfo.city = binding.city.text.toString()
        ApplicationMain.dataManager.allInfo.zip = binding.zip.text.toString()
    }
}
