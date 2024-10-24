package com.neuroid.example

import android.view.View
import android.widget.SeekBar
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

class SetSeekBarProgress(val progress: Int): ViewAction {
    override fun getConstraints(): Matcher<View> {
        return ViewMatchers.isAssignableFrom(SeekBar::class.java)
    }

    override fun getDescription(): String {
        return "Set the progress of a SeekBar"
    }

    override fun perform(uiController: UiController, view: View) {
        val seekBar = view as SeekBar
        seekBar.progress = progress
    }
}