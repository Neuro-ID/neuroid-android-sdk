package com.neuroid.example

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.neuroid.example.databinding.InstructionsBinding
import com.neuroid.tracker.NeuroID

class Instructions : AppCompatActivity() {
    private lateinit var binding: InstructionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = InstructionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        NeuroID.getInstance()?.setScreenName(this.localClassName)

        // set registered user id
        NeuroID.getInstance()?.setRegisteredUserID("${ApplicationMain.registeredUserName}${ApplicationMain.registeredUserID}")
        ApplicationMain.registeredUserID = ApplicationMain.registeredUserID + 1

        if (ApplicationMain.isApplicationSubmitted == true) {
            binding.startApplicationButton.isEnabled = false
            binding.startApplicationButton.text = "Application Submitted"
            binding.startApplicationButton.alpha = 0.5f
        }
        binding.startApplicationButton.setOnClickListener {
            NeuroID.getInstance()?.startAppFlow(siteID="form_hares612", userID="${ApplicationMain.sessionName}${ApplicationMain.registeredSessionId}")
            startActivity(Intent(this, PersonalInformation::class.java))
        }
        binding.sendMoneyButton.setOnClickListener {
            // should be using form_bench881 when resume() was hit!
            startActivity(Intent(this, SendMoney::class.java))
        }
        binding.eulaText.setOnClickListener { openTouDialog() }
    }

    private fun openTouDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.tou_dialog)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.setCancelable(true)
        val webview = dialog.findViewById<WebView>(R.id.tou_view)
        webview.webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    return true
                }
            }
        webview.loadUrl("https://www.neuro-id.com/terms-of-use")
        val dismiss = dialog.findViewById<Button>(R.id.dismiss_button)
        dismiss.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        ApplicationMain.dataManager.clearData()
        NeuroID.getInstance()?.getUserID()?.let {userId ->
            NeuroID.getInstance()?.startAppFlow("form_bench881", "${ApplicationMain.sessionName}${ApplicationMain.registeredSessionId}")
        }
    }
}
