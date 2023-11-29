package com.sample.neuroid.us.domain.config

import java.util.Date

class ConfigHelper(
    val userId: String = "${Date().time}",
    val formId: String = "form_picks709",
    val apiKey: String = "api_LIVE_hM9F3kJJiZrSvmZtUJaKaXAD"

)