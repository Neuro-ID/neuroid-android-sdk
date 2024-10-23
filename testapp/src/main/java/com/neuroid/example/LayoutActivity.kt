package com.neuroid.example

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.utils.NIDVersion

open class LayoutActivity: AppCompatActivity() {

    fun showNIDDataOnClickListener(screenName: String): OnClickListener {
        val statusListener = object : View.OnClickListener {
            override fun onClick(p0: View?) {
                val alertDialog: AlertDialog? = this.let {
                    val builder = AlertDialog.Builder(this@LayoutActivity)
                    builder.apply {
                        setMessage("NID client id:\n${NeuroID.getInstance()?.getClientID()}\n\n" +
                                "NID session id:\n${NeuroID.getInstance()?.getSessionID()}\n\n" +
                                "NID user id:\n${NeuroID.getInstance()?.getUserID()}\n\n" +
                                "NID screen:\n${screenName}\n\n" +
                                "NID SDK:\n${NeuroID?.getInstance()?.getSDKVersion()}\n\n")
                        setNegativeButton(R.string.cancel,
                            DialogInterface.OnClickListener { dialog, id ->
                                // User cancelled the dialog
                            })
                    }

                    // Create the AlertDialog
                    builder.create()
                }
                alertDialog?.show()
            }
        }
        return statusListener
    }

    fun userIDDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.user_id_dialog)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (250 * resources.displayMetrics.density).toInt(),
        )
        dialog.setCancelable(false)

        val userId = dialog.findViewById<EditText>(R.id.user_id_text)
        val saveUserId = dialog.findViewById<Button>(R.id.save_user_id)
        saveUserId.setOnClickListener {
            (application as ApplicationMain).setUserID(userId.text.toString())
            dialog.dismiss()
        }
        dialog.show()
    }
}