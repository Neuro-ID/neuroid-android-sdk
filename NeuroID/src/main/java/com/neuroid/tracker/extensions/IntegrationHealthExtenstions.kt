package com.neuroid.tracker.extensions

import android.os.Build
import com.google.gson.Gson
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.models.DeviceOrientation
import com.neuroid.tracker.models.IntegrationHealthDeviceInfo
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.utils.*
import org.json.JSONObject

internal fun generateIntegrationHealthDeviceReport() {
    val deviceInfo: IntegrationHealthDeviceInfo = IntegrationHealthDeviceInfo(
        name = "${Build.MANUFACTURER} - ${Build.BRAND} - ${Build.DEVICE} - ${Build.PRODUCT}",
        systemName = "Android SDK: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})",
        systemVersion = "${Build.VERSION.SDK_INT}",
        isSimulator = RootHelper().isProbablyEmulator(),
        Orientation = DeviceOrientation(
            rawValue = 0,
            isFlat = false,
            isLandscape = false,
            isPortrait = false,
            isValid = false
        ),
        model = Build.PRODUCT,
        type = Build.DEVICE,
        customDeviceType = "",
        nidSDKVersion = NIDVersion.getSDKVersion()
    )

    val jsonObj =
        JSONObject()
            .put("name", deviceInfo.name)
            .put("systemName", deviceInfo.systemName)
            .put("systemVersion", deviceInfo.systemVersion)
            .put("isSimulator", deviceInfo.isSimulator)
            .put("model", deviceInfo.model)
            .put("type", deviceInfo.type)
            .put("customDeviceType", deviceInfo.customDeviceType)
            .put("nidSDKVersion", deviceInfo.nidSDKVersion)


    val context = NeuroID.getInstance()?.application?.getApplicationContext()
    if (context != null) {
        createJSONFile(
            context = context,
            fileName = Constants.integrationHealthDevice.displayName,
            jsonString = jsonObj.toString()
        )
    }
}

@Synchronized internal fun generateIntegrationHealthReport(saveCopy: Boolean = false) {
    val nidInstance = NeuroID.getInstance()

    val context = nidInstance?.application?.getApplicationContext()
    if (context != null) {
        val gson = Gson()
        val events = nidInstance?.debugIntegrationHealthEvents

        var immutableList = events?.toList()

        val json: String = gson.toJson(immutableList)

        createJSONFile(
            context = context,
            fileName = Constants.integrationHealthEvents.displayName,
            jsonString = json
        )
    }
}


// Internal Extensions
internal fun NeuroID.shouldDebugIntegrationHealth(ifTrueCB: () -> Unit) {
    if (this.verifyIntegrationHealth && this.getEnvironment() == "TEST") {
        ifTrueCB()
    }
}

internal fun NeuroID.startIntegrationHealthCheck() {
    shouldDebugIntegrationHealth {
        this.debugIntegrationHealthEvents = mutableListOf<NIDEventModel>()
        generateIntegrationHealthDeviceReport()
        generateNIDIntegrationHealthReport()
    }
}

internal fun NeuroID.captureIntegrationHealthEvent(event: NIDEventModel) {
    shouldDebugIntegrationHealth {
//        NIDLog.d("NeuroID", "Adding Health Event: ${event.type}")
        this.debugIntegrationHealthEvents.add(event)
    }
}

internal fun NeuroID.getIntegrationHealthEvents(): List<NIDEventModel> {
    return this.debugIntegrationHealthEvents
}

internal fun NeuroID.saveIntegrationHealthEvents() {
    shouldDebugIntegrationHealth {
        generateNIDIntegrationHealthReport()
    }
}

internal fun NeuroID.generateNIDIntegrationHealthReport(saveIntegrationHealthReport: Boolean = false) {
    shouldDebugIntegrationHealth {
        generateIntegrationHealthReport(saveCopy = saveIntegrationHealthReport)
    }
}


// Public Extensions
fun NeuroID.printIntegrationHealthInstruction() {
    NIDLog.d(
        "NeuroID",
        "ℹ️ NeuroID Integration Health Instructions:\n" +
                "1. Open Android Studio\n" +
                "2. Go to View -> Tool Windows -> Device File Explorer\n" +
                "3. Select your emulator and navigate to `data/data/YOUR_APP_NAME/files/nid`\n" +
                "4. Right click on the `nid` directory and save to a local folder on your system\n" +
                "5. Open a terminal prompt\n" +
                "6. Cd to the directory you just saved the folder to\n" +
                "7. Cd to `nid`\n" +
                "8. Run `node server.js`"
    )

    val context = NeuroID.getInstance()?.application?.getApplicationContext()
    if (context != null) {
        copyDirorfileFromAssetManager(
            context = context,
            arg_assetDir = Constants.integrationHealthAssetsFolder.displayName,
            arg_destinationDir = Constants.integrationHealthFolder.displayName
        )
    }

}

fun NeuroID.setVerifyIntegrationHealth(verify: Boolean) {
    this.verifyIntegrationHealth = verify

    if (verify) {
        printIntegrationHealthInstruction()
    }
}