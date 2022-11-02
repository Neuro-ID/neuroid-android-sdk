package com.sample.neuroid.us.activities

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.sample.neuroid.us.R
import com.sample.neuroid.us.databinding.NidActivityDynamicBinding


class DynamicActivity : AppCompatActivity() {
    private lateinit var binding: NidActivityDynamicBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.nid_activity_dynamic
        )
        binding.apply {
            btnAdd.setOnClickListener {
                addNewView(llContainer)
            }
        }
    }

    private fun addNewView(viewGroup: ViewGroup) {
        val editText = EditText(this)
        editText.tag = "etNewEditText"
        editText.setText("New EditText")
        viewGroup.addView(editText)
        val button = Button(this)
        button.tag = "btnNewButton"
        button.text = "New Button"

        viewGroup.addView(button)
    }
}