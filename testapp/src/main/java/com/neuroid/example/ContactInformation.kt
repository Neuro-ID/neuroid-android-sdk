package com.neuroid.example

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.neuroid.example.databinding.ContactInformationBinding
import com.neuroid.tracker.NeuroID

class ContactInformation : LayoutActivity() {
    private lateinit var binding: ContactInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // since stop clears user id, we store the user entered id outside of the SDK
        // (in the ApplicationMain.userEneteredId) for reuse here!
        binding = ContactInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        NeuroID.getInstance()?.setScreenName(this.localClassName)
        binding.header.neuroidTitle.setOnClickListener(
            showNIDDataOnClickListener(this.localClassName),
        )
        val home = Intent(this, Instructions::class.java)
        val next = Intent(this, AddressInformation::class.java)
        val previous = Intent(this, LoanApplication::class.java)
        val adapter = AdvertiserAdapter()
        showBankList(binding.switch1.isChecked)
        binding.switch1.setOnClickListener {
            showBankList(binding.switch1.isChecked)
        }
        binding.banksList.layoutManager = LinearLayoutManager(this)
        binding.banksList.adapter = adapter
        adapter.setBanks(
            arrayOf(
                "Wells Fargo",
                "US Bank",
                "Bank of the West",
                "Chase",
                "The Fed",
                "Freddie Mac",
                "Fannie Mae",
                "PayPal",
                "First Hawaiian Bank",
                "Bank of Hawaii",
                "Territorial Savings",
                "Union Bank of California",
                "Bank Of China",
                "Chemical Bank",
                "Mechanics Bank",
            ),
        )
        binding.header.pageLabel.text = getString(R.string.contact_information)
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

    fun showBankList(isClicked: Boolean) {
        if (isClicked) {
            binding.banksList.visibility = View.VISIBLE
            binding.textView7.visibility = View.VISIBLE
        } else {
            binding.banksList.visibility = View.GONE
            binding.textView7.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        binding.email.setText(ApplicationMain.dataManager.allInfo.email)
        binding.phone.setText(ApplicationMain.dataManager.allInfo.phone)
    }

    fun updateData() {
        ApplicationMain.dataManager.allInfo.email = binding.email.text.toString()
        ApplicationMain.dataManager.allInfo.phone = binding.phone.text.toString()
    }
}
