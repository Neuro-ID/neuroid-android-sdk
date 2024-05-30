package com.sample.neuroid.us.domain.config

import java.util.Date

class ConfigHelper(
    val userId: String = "${Date().time}",
    val formId: String = "form_skein469",
    val apiKey: String = "key_live_MwC5DQNYzRsRhnnYjvz1fJtp"

)