package com.neuroid.tracker.events

const val CREATE_SESSION = "CREATE_SESSION"
const val MOBILE_METADATA_ANDROID = "MOBILE_METADATA_ANDROID"
const val CLOSE_SESSION = "CLOSE_SESSION"
const val SET_USER_ID = "SET_USER_ID"
const val SET_REGISTERED_USER_ID = "SET_REGISTERED_USER_ID"
const val HEARTBEAT = "HEARTBEAT"
const val ERROR = "ERROR"
const val LOG = "LOG"
const val USER_INACTIVE = "USER_INACTIVE"
const val REGISTER_COMPONENT = "REGISTER_COMPONENT"
const val REGISTER_TARGET = "REGISTER_TARGET"
const val REGISTER_STYLESHEET = "REGISTER_STYLESHEET"
const val MUTATION_INSERT = "MUTATION_INSERT"
const val MUTATION_REMOVE = "MUTATION_REMOVE"
const val MUTATION_ATTR = "MUTATION_ATTR"
const val FORM_SUBMIT = "APPLICATION_SUBMIT"
const val FORM_RESET = "FORM_RESET"
const val FORM_SUBMIT_SUCCESS = "APPLICATION_SUBMIT_SUCCESS"
const val FORM_SUBMIT_FAILURE = "APPLICATION_SUBMIT_FAILURE"
const val APPLICATION_SUBMIT = "APPLICATION_SUBMIT"
const val APPLICATION_SUBMIT_SUCCESS = "APPLICATION_SUBMIT_SUCCESS"
const val APPLICATION_SUBMIT_FAILURE = "APPLICATION_SUBMIT_FAILURE"
const val PAGE_SUBMIT = "PAGE_SUBMIT"
const val FOCUS = "FOCUS"
const val BLUR = "BLUR"
const val COPY = "COPY"
const val CUT = "CUT"
const val PASTE = "PASTE"
const val INPUT = "INPUT"
const val INVALID = "INVALID"
const val KEY_DOWN = "KEY_DOWN"
const val KEY_UP = "KEY_UP"
const val CHANGE = "CHANGE"
const val SELECT_CHANGE = "SELECT_CHANGE"
const val TEXT_CHANGE = "TEXT_CHANGE"
const val RADIO_CHANGE = "RADIO_CHANGE"
const val CHECKBOX_CHANGE = "CHECKBOX_CHANGE"
const val INPUT_CHANGE = "INPUT_CHANGE"
const val SLIDER_CHANGE = "SLIDER_CHANGE"
const val SLIDER_SET_MIN = "SLIDER_SET_MIN"
const val SLIDER_SET_MAX = "SLIDER_SET_MAX"
const val TOUCH_START = "TOUCH_START"
const val TOUCH_MOVE = "TOUCH_MOVE"
const val TOUCH_END = "TOUCH_END"
const val TOUCH_CANCEL = "TOUCH_CANCEL"
const val WINDOW_LOAD = "WINDOW_LOAD"
const val WINDOW_UNLOAD = "WINDOW_UNLOAD"
const val WINDOW_FOCUS = "WINDOW_FOCUS"
const val WINDOW_BLUR = "WINDOW_BLUR"
const val WINDOW_ORIENTATION_CHANGE = "WINDOW_ORIENTATION_CHANGE"
const val WINDOW_RESIZE = "WINDOW_RESIZE"
const val DEVICE_MOTION = "DEVICE_MOTION"
const val SWITCH_CHANGE = "SWITCH_CHANGE"
const val TOGGLE_BUTTON_CHANGE = "TOGGLE_BUTTON_CHANGE"
const val RATING_BAR_CHANGE = "RATING_BAR_CHANGE"
const val CONTEXT_MENU = "CONTEXT_MENU"
const val ADVANCED_DEVICE_REQUEST = "ADVANCED_DEVICE_REQUEST"
const val ANDROID_URI = "android://"
const val SET_VARIABLE = "SET_VARIABLE"
const val CALL_IN_PROGRESS = "CALL_IN_PROGRESS"
const val NETWORK_STATE = "NETWORK_STATE"
const val CADENCE_READING_ACCEL = "CADENCE_READING_ACCEL"
const val LOW_MEMORY = "LOW_MEMORY"
const val FULL_BUFFER = "FULL_BUFFER"
const val OUT_OF_MEMORY = "OUT_OF_MEMORY"
const val ATTEMPTED_LOGIN = "ATTEMPTED_LOGIN"

// NID origin codes
const val NID_ORIGIN_NID_SET = "nid"
const val NID_ORIGIN_CUSTOMER_SET = "customer"
const val NID_ORIGIN_CODE_FAIL = "400"
const val NID_ORIGIN_CODE_NID = "200"
const val NID_ORIGIN_CODE_CUSTOMER = "201"

// Telephony Manager Call State Values https://developer.android.com/reference/android/telephony/TelephonyManager#CALL_STATE_IDLE
enum class CallInProgress(val event: String, val state: Int) {
    ACTIVE("true", 2),
    INACTIVE("false", 0),
    UNAUTHORIZED("unauthorized", 99),
}
