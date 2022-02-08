# Neuro-ID Mobile SDK for Android Demo
Neuro-ID's Mobile SDK makes it simple to embed behavioral analytics inside your mobile app. With a few lines of code, you can connect your app with the Neuro-ID platform and make informed decisions about your users.

## Getting Started
### Local Settings
1. Add .aar file in the libs folder

2. In the application gradle add the dependencies:
* QA Environment:
```gradle
implementation files('libs/neuro-id-android-v1.0-debug.aar')
implementation "androidx.security:security-crypto:1.1.0-alpha03"
```

* Production Environment:
```gradle
implementation files('libs/neuro-id-android-v1.0-release.aar')
implementation "androidx.security:security-crypto:1.1.0-alpha03"
```

3. In your application:
```kotlin
import androidx.multidex.MultiDexApplication
import com.neuroid.tracker.NeuroID
class MyApplication: MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        val neuroId = NeuroID.Builder(this, "key_live_vtotrandom_form_mobilesandbox")
            .setTimeInSeconds(5)
            .build()
        NeuroID.setNeuroIdInstance(neuroId) // Automatically save the events
        NeuroID.getInstance().start() // Start to send all events saved to server
    }
}
```

## Sample application
If you want to see the demo of this proyect deployed, just launch the applicaion "NeuroIdAndroidExample"

## License
The Neuro-ID Mobile SDK is provided under an [MIT License](LICENSE).
