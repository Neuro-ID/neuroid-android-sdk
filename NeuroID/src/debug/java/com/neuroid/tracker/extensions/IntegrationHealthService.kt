package com.neuroid.tracker.extensions

import android.os.Build
import com.google.gson.Gson
import com.neuroid.tracker.NeuroID
import com.neuroid.tracker.NeuroIDPublic
import com.neuroid.tracker.models.DeviceOrientation
import com.neuroid.tracker.models.IntegrationHealthDeviceInfo
import com.neuroid.tracker.models.IntegrationHealthProtocol
import com.neuroid.tracker.models.NIDEventModel
import com.neuroid.tracker.utils.*
import org.json.JSONObject

internal fun generateIntegrationHealthDeviceReport() {
    val deviceInfo =
        IntegrationHealthDeviceInfo(
            name = "${Build.MANUFACTURER} - ${Build.BRAND} - ${Build.DEVICE} - ${Build.PRODUCT}",
            systemName = "Android SDK: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})",
            systemVersion = "${Build.VERSION.SDK_INT}",
            isSimulator = RootHelper().isProbablyEmulator(),
            Orientation =
                DeviceOrientation(
                    rawValue = 0,
                    isFlat = false,
                    isLandscape = false,
                    isPortrait = false,
                    isValid = false,
                ),
            model = Build.PRODUCT,
            type = Build.DEVICE,
            customDeviceType = "",
            nidSDKVersion = NeuroID.getInstance()?.getSDKVersion()?:""
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

    val context = NeuroID.getInternalInstance()?.getApplicationContext()
    if (context != null) {
        createJSONFile(
            context = context,
            fileName = Constants.integrationHealthDevice.displayName,
            jsonString = jsonObj.toString(),
        )
    }
}

internal class IntegrationHealthService(
    private val logger: NIDLogWrapper,
    private val neuroID: NeuroID,
): IntegrationHealthProtocol {

    internal var verifyIntegrationHealth: Boolean = false
    internal var debugIntegrationHealthEvents: MutableList<NIDEventModel> =
        mutableListOf<NIDEventModel>()

    private fun shouldDebugIntegrationHealth(ifTrueCB: () -> Unit) {
        if (this.verifyIntegrationHealth) {
            ifTrueCB()
        }
    }

    override fun startIntegrationHealthCheck() {
        shouldDebugIntegrationHealth {
            this.debugIntegrationHealthEvents = mutableListOf<NIDEventModel>()
            generateIntegrationHealthDeviceReport()
            generateNIDIntegrationHealthReport()
        }
    }

    override fun captureIntegrationHealthEvent(event: NIDEventModel) {
        shouldDebugIntegrationHealth {
            this.debugIntegrationHealthEvents.add(event)
        }
    }
    internal fun getIntegrationHealthEvents(): List<NIDEventModel> {
        return this.debugIntegrationHealthEvents
    }

     override fun saveIntegrationHealthEvents() {
        shouldDebugIntegrationHealth {
            generateNIDIntegrationHealthReport()
        }
    }

    @Synchronized
    fun generateNIDIntegrationHealthReport(saveIntegrationHealthReport: Boolean = false) {
        shouldDebugIntegrationHealth {
            val context = neuroID.application?.applicationContext
            if (context != null) {
                val gson = Gson()
                val events = this.debugIntegrationHealthEvents

                val immutableList = events.toList()

                val json: String = gson.toJson(immutableList)

                createJSONFile(
                    context = context,
                    fileName = Constants.integrationHealthEvents.displayName,
                    jsonString = json,
                )
            }
        }
    }

    // Public Methods for User
    override fun printIntegrationHealthInstruction() {
        logger.i(
            "NeuroID",
            "ℹ️ NeuroID Integration Health Instructions:\n" +
                    "1. Open Android Studio\n" +
                    "2. Go to View -> Tool Windows -> Device File Explorer\n" +
                    "3. Select your emulator and navigate to `data/data/YOUR_APP_NAME/files/nid`\n" +
                    "4. Right click on the `nid` directory and save to a local folder on your system\n" +
                    "5. Open a terminal prompt\n" +
                    "6. Cd to the directory you just saved the folder to\n" +
                    "7. Cd to `nid`\n" +
                    "8. Run `node server.js`",
        )

        val context = neuroID.application?.applicationContext
        if (context != null) {
            copyDirorfileFromAssetManager(
                context = context,
                arg_assetDir = Constants.integrationHealthAssetsFolder.displayName,
                arg_destinationDir = Constants.integrationHealthFolder.displayName,
            )
        }
    }

    override fun setVerifyIntegrationHealth(verify: Boolean) {
        this.verifyIntegrationHealth = verify

        if (verify) {
            printIntegrationHealthInstruction()
        }
    }
}
// Public Extensions
fun NeuroIDPublic.printIntegrationHealthInstruction() {
    this.printIntegrationHealthInstruction()
}

fun NeuroIDPublic.setVerifyIntegrationHealth(verify: Boolean) {
    this.setVerifyIntegrationHealth(verify)
}
