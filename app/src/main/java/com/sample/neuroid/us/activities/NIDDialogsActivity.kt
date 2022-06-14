package com.sample.neuroid.us.activities

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.sample.neuroid.us.R
import com.sample.neuroid.us.databinding.NidActivityDialogsBinding
import java.util.*

class NIDDialogsActivity: AppCompatActivity() {
    private lateinit var binding: NidActivityDialogsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.nid_dialogs_activity_title)
        binding = DataBindingUtil.setContentView(this, R.layout.nid_activity_dialogs)
        binding.apply {
            buttonShowAlertDialog.setOnClickListener {

                val dialog = AlertDialog.Builder(this@NIDDialogsActivity)
                dialog.setTitle("Alert dialog")
                dialog.setMessage("This is a simple alert dialog with two options!")
                dialog.setPositiveButton("Yes"
                ) { dialogInt, _ ->
                    dialogInt.dismiss()
                    Toast.makeText(this@NIDDialogsActivity, "You selected yes option", Toast.LENGTH_LONG).show()
                }
                dialog.setNegativeButton("No"
                ) { dialogInt, _ ->
                    dialogInt.dismiss()
                    Toast.makeText(this@NIDDialogsActivity, "You selected no option", Toast.LENGTH_LONG).show()
                }

                dialog.show()
            }

            buttonShowCustomDialog.setOnClickListener {
                val customDialog = Dialog(this@NIDDialogsActivity)
                customDialog.setContentView(R.layout.nid_dialog_custom_message)
                customDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                val buttonClose = customDialog.findViewById<ImageButton>(R.id.imageButton_close)
                val buttonOk = customDialog.findViewById<Button>(R.id.button_ok)

                buttonClose.setOnClickListener {
                    customDialog.dismiss()
                }

                buttonOk.setOnClickListener {
                    customDialog.dismiss()
                }

                customDialog.show()
            }

            buttonShowDatePicker.setOnClickListener {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                val datePicker = DatePickerDialog(
                    this@NIDDialogsActivity,
                    { _, y, m, d ->
                        Toast.makeText(this@NIDDialogsActivity, "Date Selected: $y, $m, $d", Toast.LENGTH_LONG).show()
                    },
                    year,
                    month,
                    day)


                datePicker.show()
            }

            buttonShowTimePicker.setOnClickListener {
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR)
                val minute = calendar.get(Calendar.MINUTE)

                val timePicker = TimePickerDialog(
                    this@NIDDialogsActivity,
                    { _, h, m ->
                        Toast.makeText(this@NIDDialogsActivity, "Time Selected: $h, $m", Toast.LENGTH_LONG).show()
                    },
                    hour,
                    minute,
                    false
                )

                timePicker.show()
            }

        }
    }
}