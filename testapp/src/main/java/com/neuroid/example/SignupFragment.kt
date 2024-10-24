package com.neuroid.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.neuroid.example.databinding.SignUpBinding
import com.neuroid.tracker.NeuroID

class SignupFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = SignUpBinding.inflate(inflater, container, false)
        setupUI(binding)
        return binding.root
    }

    fun setupUI(binding: SignUpBinding) {
        NeuroID.getInstance()?.setScreenName((activity as SignUp).localClassName)
        val headerText = binding.header.pageLabel
        headerText.text = "Signup"
        val continueButton = binding.continueButton
        val previous= binding.header.previous
        val cancel = binding.cancelButton
        val email = binding.email
        previous.setOnClickListener {
            (activity as SignUp).onCancelPreviousButton()
        }
        cancel.setOnClickListener {
            (activity as SignUp).onCancelPreviousButton()
        }
        continueButton.setOnClickListener {
            (activity as SignUp).onContinueButtonFirst(email.text.toString())
        }
    }
}