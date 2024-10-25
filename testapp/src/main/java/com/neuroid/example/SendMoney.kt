package com.neuroid.example

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import com.neuroid.example.databinding.SendMoneyBinding
import com.neuroid.tracker.NeuroID

class SendMoney: LayoutActivity() {
    private lateinit var binding: SendMoneyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NeuroID.getInstance()?.setScreenName(this.localClassName)
        binding = SendMoneyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.submitButton.setOnClickListener{
            startActivity(Intent(this, Instructions::class.java))
        }

        binding.header.pageLabel.setText("Send Money")
        binding.header.previous.setOnClickListener {
            startActivity(Intent(this, Instructions::class.java))
        }
        binding.sendAmountScrubber.setOnSeekBarChangeListener(
            object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    p0: SeekBar?,
                    p1: Int,
                    p2: Boolean,
                ) {
                    p0?.progress?.let {
                        binding.sendAmountLabel.text = "Send Amount $${it * 1000}"
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