package com.sample.neuroid.us

import org.junit.Assert

const val NID_STRUCT_CREATE_SESSION = "\\{\"type\":\"CREATE_SESSION\",\"ts\":\\d{13,},\"f\":\"(.*?)\",\"sid\":\"(.*?)\",\"cid\":\"(.*?)\",\"did\":\"(.*?)\",\"iid\":\"(.*?)\",\"loc\":\"(.*?)\",\"ua\":\"(.*?)\",\"tzo\":(.*?),\"lng\":\"(.*?)\",\"ce\":true,\"je\":true,\"ol\":true,\"p\":\"Android\",\"dnt\":false,\"url\":\"(.*?)\",\"ns\":\"nid\",\"jsl\":\\[\\],\"jsv\":\"(.*?)\",\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_USER_ID = "\\{\"type\":\"SET_USER_ID\",\"ts\":(.*?),\"uid\":\"(.*?)\",\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_REGISTER_TARGET = "\\{\"type\":\"REGISTER_TARGET\",\"tg\":\\{\"attr\":\\{(.*?)\\}\\},\"tgs\":\"(.*?)\",\"v\":\"S~C~~\\d{1,}\",\"en\":\"(.*?)\",\"et\":\"(.*?)\",\"eid\":\"(.*?)\",\"ts\":\\d{13,},\"url\":\"(.*?)\",\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_SLIDER_CHANGE = "\\{\"type\":\"SLIDER_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"etn\":\"(.*?)\"\\},\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_FORM_SUBMIT = "\\{\"type\":\"APPLICATION_SUBMIT\",\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_FORM_SUCCESS = "\\{\"type\":\"APPLICATION_SUBMIT_SUCCESS\",\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_FORM_ERROR = "\\{\"type\":\"APPLICATION_SUBMIT_FAILURE\",\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_CUSTOM_EVENT = "\\{\"type\":\"CUSTOM_EVENT\",\"tgs\":\"(.*?)\",\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_WINDOW_LOAD = "\\{\"type\":\"WINDOW_LOAD\",\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_WINDOW_FOCUS = "\\{\"type\":\"WINDOW_FOCUS\",\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_WINDOW_BLUR = "\\{\"type\":\"WINDOW_BLUR\",\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_WINDOW_UNLOAD = "\\{\"type\":\"WINDOW_UNLOAD\",\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_TOUCH_START = "\\{\"type\":\"TOUCH_START\",\"tg\":\\{\"tgs\":\"\"\\},\"touches\":\\[\\{\"tid\":0,\"x\":(.*?),\"y\":(.*?)\\}\\],\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_TOUCH_END = "\\{\"type\":\"TOUCH_END\",\"tg\":\\{\"tgs\":\"\"\\},\"touches\":\\[\\{\"tid\":0,\"x\":(.*?),\"y\":(.*?)\\}\\],\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_TOUCH_MOVE = "\\{\"type\":\"TOUCH_MOVE\",\"tg\":\\{\"tgs\":\"\"\\},\"touches\":\\[\\{\"tid\":0,\"x\":(.*?),\"y\":(.*?)\\}\\],\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_WINDOW_RESIZE = "\\{\"type\":\"WINDOW_RESIZE\",\"ts\":\\d{13,},\"w\":\\d+,\"h\":\\d+,\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_WINDOW_ORIENTATION_CHANGE = "\\{\"type\":\"WINDOW_ORIENTATION_CHANGE\",\"tg\":\\{\"orientation\":\"(?:Landscape|Portrait)\"\\},\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_FOCUS = "\\{\"type\":\"FOCUS\",\"tg\":\\{\"tgs\":\"(.*?)\"\\},\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_BLUR = "\\{\"type\":\"BLUR\",\"tg\":\\{\"tgs\":\"(.*?)\"\\},\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_INPUT = "\\{\"type\":\"INPUT\",\"tg\":\\{\"tgs\":\"(.*?)\",\"attr\":\\{(.*?)\\},\"etn\":\"(.*?)\",\"et\":\"(.*?)\"\\},\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_TEXT_CHANGE = "\\{\"type\":\"TEXT_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"attr\":\\{(.*?)\\},\"etn\":\"(.*?)\",\"et\":\"(.*?)\"\\},\"v\":\"S~C~~\\d{1,}\",\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_RADIO_CHANGE = "\\{\"type\":\"RADIO_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"etn\":\"(.*?)\"\\},\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_CHECKBOX_CHANGE = "\\{\"type\":\"CHECKBOX_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"etn\":\"(.*?)\"\\},\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_SELECT_CHANGE = "\\{\"type\":\"SELECT_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"etn\":\"(.*?)\",\"et\":\"(.*?)\"\\},\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_USER_INACTIVE = "\\{\"type\":\"USER_INACTIVE\",\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_SWITCH_CHANGE = "\\{\"type\":\"SWITCH_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"etn\":\"(.*?)\"\\},\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_TOGGLE_CHANGE = "\\{\"type\":\"TOGGLE_BUTTON_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"etn\":\"(.*?)\"\\},\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"
const val NID_STRUCT_RATING_CHANGE = "\\{\"type\":\"RATING_BAR_CHANGE\",\"tg\":\\{\"tgs\":\"(.*?)\",\"etn\":\"(.*?)\"\\},\"ts\":\\d{13,},\"gyro\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\},\"accel\":\\{\"x\":(.*?),\"y\":(.*?),\"z\":(.*?)\\}\\}"


fun validateEventCount(
    eventList: Set<String>,
    eventType: String,
    maxEventsCount: Int = 1
): String {
    val events = eventList.filter { it.contains(eventType) }
    if (maxEventsCount > 0) {
        Assert.assertEquals(maxEventsCount, events.size)
    }
    return events.first()
}