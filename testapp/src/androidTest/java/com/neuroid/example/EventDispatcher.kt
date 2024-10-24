package com.neuroid.example

import com.google.gson.Gson
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.io.BufferedReader
import java.io.InputStreamReader


/**
 * handle the requests from the app and respond, in this instance, for
 * collection requests, we will always send back success and record
 * all requests for verification. for remote config requests, we send back
 * a predefined remote configuration.
 */
class EventDispatcher(private val eventRecorder: EventRecorder): Dispatcher() {

    private val remoteConfigResponse = "config response\n" +
            "{\n" +
            "  \"call_in_progress\": true,\n" +
            "  \"event_queue_flush_interval\": 1,\n" +
            "  \"event_queue_flush_size\": 2000,\n" +
            "  \"geo_location\": false,\n" +
            "  \"gyro_accel_cadence\": false,\n" +
            "  \"gyro_accel_cadence_time\": 200,\n" +
            "  \"is_mobile_active\": true,\n" +
            "  \"request_timeout\": 10,\n" +
            "  \"site_id\": \"form_skein469\",\n" +
            "  \"linked_site_options\": {\n" +
            "    \"form_parks912\": {\n" +
            "      \"sample_rate\": 50\n" +
            "    },\n" +
            "    \"form_bench881\": {\n" +
            "      \"sample_rate\": 0\n" +
            "    },\n" +
            "    \"form_hares612\": {\n" +
            "      \"sample_rate\": 100\n" +
            "    }\n" +
            "  }\n" +
            "}"

    private val collectorResponse = "collection response\n" +
            "{\n" +
            "  \"status\": \"OK\"\n" +
            "}"

    override fun dispatch(request: RecordedRequest): MockResponse {
        return if (request.requestUrl.toString().contains("/mobile/")) {
            MockResponse().setResponseCode(200).setBody(remoteConfigResponse)
        } else {
            val gson = Gson()
            val eventModel = gson.fromJson(BufferedReader(InputStreamReader(request.body.inputStream())),
                EventModel::class.java)
            eventRecorder.addEvent(eventModel)
            MockResponse().setResponseCode(200).setBody(collectorResponse)
        }
    }
}