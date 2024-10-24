package com.neuroid.example

import android.content.Intent
import android.os.Bundle

class SignUp: LayoutActivity() {

    val signupFragment = SignupFragment()
    val personalInformationTwoFragment = PersonalInformationTwoFragment()
    val personalInformationThreeFragment = PersonalInformationThreeFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_fragment)
        supportFragmentManager.beginTransaction().add(R.id.signup_fragment, signupFragment).commit()
    }

    fun onCancelPreviousButton() {
        startActivity(Intent(this, Splash::class.java))
    }

    fun onContinueButtonFirst(userId: String) {
        (application as ApplicationMain).setUserID(userId)
        val t = supportFragmentManager.beginTransaction()
        t.replace(R.id.signup_fragment, personalInformationTwoFragment)
        t.addToBackStack(null)
        t.commit()
    }
    fun onContinueButtonSecond(userId: String) {
        (application as ApplicationMain).setUserID(userId)
        val t = supportFragmentManager.beginTransaction()
        t.replace(R.id.signup_fragment, personalInformationThreeFragment)
        t.addToBackStack(null)
        t.commit()
    }

    fun onSignupButton() {
        startActivity(Intent(this, Instructions::class.java))
    }

    fun onPreviousPersonalInfoTwo() {
        val t = supportFragmentManager.beginTransaction()
        t.replace(R.id.signup_fragment, signupFragment)
        t.addToBackStack(null)
        t.commit()
    }

    fun onPreviousPersonalInfoThree() {
        val t = supportFragmentManager.beginTransaction()
        t.replace(R.id.signup_fragment, personalInformationTwoFragment)
        t.addToBackStack(null)
        t.commit()
    }

    fun updateDOB(dob: String) {
        ApplicationMain.dataManager.allInfo.dob = dob
    }
}