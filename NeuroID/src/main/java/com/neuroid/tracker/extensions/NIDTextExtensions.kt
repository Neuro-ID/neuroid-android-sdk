package com.neuroid.tracker.extensions

import android.util.Base64
import android.util.TypedValue
import android.view.View
import kotlin.math.roundToInt

fun View.dpToPx(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).roundToInt()

fun String.encodeToBase64(): String =
    Base64.encodeToString(this.toByteArray(Charsets.UTF_8), Base64.DEFAULT)