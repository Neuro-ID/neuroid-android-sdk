package com.neuroid.tracker.utils

import android.content.res.Resources
import java.util.Locale

class NIDResourcesUtils {
    fun getDisplayMetricsWidth() = Resources.getSystem().displayMetrics.widthPixels
    fun getDisplayMetricsHeight() = Resources.getSystem().displayMetrics.heightPixels
    fun getHttpAgent() = System.getProperty("http.agent").orEmpty()
    fun getDefaultLocale() = Locale.getDefault().toString()
    fun getDefaultLanguage(): String = Locale.getDefault().language
}