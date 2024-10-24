package com.neuroid.example

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.SeekBar
import com.neuroid.example.databinding.LoanApplicationBinding
import com.neuroid.tracker.NeuroID

class LoanApplication : LayoutActivity() {
    private lateinit var binding: LoanApplicationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoanApplicationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        NeuroID.getInstance()?.setScreenName(this.localClassName)
        binding.header.neuroidTitle.setOnClickListener(
            showNIDDataOnClickListener(this.localClassName),
        )
        binding.header.pageLabel.setText("Loan Application")
        val home = Intent(this, Instructions::class.java)
        val next = Intent(this, ContactInformation::class.java)
        val previous = Intent(this, PersonalInformation::class.java)
        val interestRates = listOf("2%", "3%", "4%", "5%", "6%", "7%", "8%", "9%", "10%")
        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, interestRates)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.interestRateSpinner.adapter = arrayAdapter

        binding.header.previous.setOnClickListener {
            startActivity(previous)
        }
        binding.footer.home.setOnClickListener {
            (application as ApplicationMain).stopTracking()
            startActivity(home)
        }
        binding.footer.next.setOnClickListener {
            startActivity(next)
        }
        binding.loanLengthDecrement.setOnClickListener {
            val amount = Integer.parseInt(binding.loanLength.text.toString())
            if (amount > 0) {
                binding.loanLength.text = (amount - 1).toString()
            }
        }

        binding.loanLengthIncrement.setOnClickListener {
            val amount = Integer.parseInt(binding.loanLength.text.toString())
            if (amount < 30) {
                binding.loanLength.text = (amount + 1).toString()
            }
        }

        binding.loanAmountScrubber.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    p0: SeekBar?,
                    p1: Int,
                    p2: Boolean,
                ) {
                    p0?.progress?.let {
                        binding.loanAmount.text = "Loan Amount $${it * 1000}"
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                }
            },
        )
    }
}
