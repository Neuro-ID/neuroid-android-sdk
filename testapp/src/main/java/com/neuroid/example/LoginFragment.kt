package com.neuroid.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedTextField

import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.neuroid.example.databinding.LoginBinding
import com.neuroid.tracker.NeuroID

class LoginFragment: Fragment() {

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        NeuroID.getInstance()?.setScreenName((activity as Login).localClassName)
        val binding = LoginBinding.inflate(layoutInflater, container, false)
        setupUiWithBinding(binding)
        return binding.root
    }

    fun setupUiWithBinding(binding: LoginBinding) {
        NeuroID.getInstance()?.setScreenName((activity as Login).localClassName)
        val headerText = binding.header.pageLabel
        headerText.text = "Compose Login"
        val previous = binding.header.previous
        previous.setOnClickListener {
            (activity as Login).goBack()
        }
        binding.composeView.setContent {
            LoginView()
        }
    }

    @Composable
    fun LoginView() {
        val colorBackground = activity?.resources?.getColor(R.color.nid_green_light, context?.theme)?:0
        val colorText = activity?.resources?.getColor(R.color.white, context?.theme)?:0

        var email by remember { mutableStateOf(TextFieldValue(""))}
        var password  by remember { mutableStateOf(TextFieldValue("")) }

        // *** here is our trackPageElements() call ***
        NeuroID.compose?.trackPageElements(
            pageName = "loginPage",
            elements = arrayOf(
                Pair(first = "email", second = email.text),
                Pair(first = "pasword", second = password.text),
            )
        )

        Row {
            Column(modifier = Modifier
                .padding(40.dp)
                .fillMaxWidth()) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it },
                    modifier = Modifier.testTag("email"),
                    label = {Text("email", color = Color(colorBackground))},
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(colorBackground),
                        cursorColor = Color(colorBackground)
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it},
                    modifier = Modifier.testTag("password"),
                    visualTransformation = PasswordVisualTransformation(),
                    label = {Text("password", color = Color(colorBackground))},
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(colorBackground),
                        cursorColor = Color(colorBackground)
                    )
                )
                Row (modifier = Modifier
                    .padding(40.dp)
                    .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        modifier = Modifier.testTag("login_button"),
                        onClick = {
                            NeuroID.compose?.trackButtonTap(
                                "login",
                                "login page")
                            (activity as Login).login(email.text) },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(colorBackground),
                            contentColor = Color(colorText))
                    ) {
                        Text(text = "login")
                    }
                    Button(
                        onClick = {
                            NeuroID.compose?.trackButtonTap(
                                "back",
                                "login page")
                            (activity as Login).goBack() },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(colorBackground),
                            contentColor = Color(colorText))
                    ) {
                        Text(text = "cancel")
                    }
                }
            }
        }
    }
}



