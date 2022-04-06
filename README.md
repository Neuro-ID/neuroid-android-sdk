# Neuro-ID Mobile SDK for Android Demo
Neuro-ID's Mobile SDK makes it simple to embed behavioral analytics inside your mobile app. With a few lines of code, you can connect your app with the Neuro-ID platform and make informed decisions about your users.

## Getting Started

### Requirements
* minSDK 21 Supported

### Mobile SDK and App Demo
In this project you will find the source code of the library, and there is also a demo application in which the NeuroID library is already integrated.

### Install the library
1. Copy the .aar file found in the libs folder of the demo application to the libs folder of your project. There are two files:
* libs/neuro-id-android-v1.0-debug.aar: Debug and QA Environment
* libs/neuro-id-android-v1.0-release.aar: Release and Production Environment

2. In your application gradle add the dependencies:
* QA Environment:
```gradle
implementation('org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2')
implementation files('libs/neuro-id-android-v1.0-debug.aar')
implementation 'androidx.security:security-crypto:1.1.0-alpha03'
```

* Production Environment:
```gradle
implementation('org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2')
implementation files('libs/neuro-id-android-v1.0-release.aar')
implementation 'androidx.security:security-crypto:1.1.0-alpha03'
```

* If your project uses 100% Java, you maybe add the dependency:
```gradle
implementation 'androidx.core:core-ktx:1.7.0'
```

3. In your application:
```kotlin
import androidx.multidex.MultiDexApplication
import com.neuroid.tracker.NeuroID

class MyApplication: MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        val neuroId = NeuroID.Builder(
            this, 
            "key_license"
        )
            .build()
        NeuroID.setNeuroIdInstance(neuroId) // Automatically save the events
        NeuroID.getInstance().start() // Start to send all events saved to server
    }
}
```
```java
import androidx.multidex.MultiDexApplication;
import com.neuroid.tracker.NeuroID;

public class MyApplication extends MultiDexApplication {
    @Override
    public void onCreate(){
        super.onCreate();

        NeuroID neuroId = new NeuroID.Builder(
                this,
                "key_live_vtotrandom_form_mobilesandbox"
        ).build();
        NeuroID.setNeuroIdInstance(neuroId);
        NeuroID.getInstance().start();
    }
}
```
Note: If it doesn't exist, you must create it and add it in the AndroidManifest.xml

## Stopping the SDK
When Neuro-ID will no longer be needed for tracking call the stop() method below. This will completely prevent any subsequent tracking within the application by Neuro-ID.
```kotlin
NeuroID.getInstance().stop()
```
```java
NeuroID.getInstance().stop();
```

## Form Submit
Whenever a user completes a form, the following methods need to be called in order to capture the conclusion of the session.

### formSubmit
This should be called whenever a form is submitted.
```kotlin
NeuroID.getInstance().formSubmit()
```
```java
NeuroID.getInstance().formSubmit();
```

### formSubmitFailure
This should be called whenever a form is rejected for any reason.
```kotlin
NeuroID.getInstance().formSubmitFailure()
```
```java
NeuroID.getInstance().formSubmitFailure();
```

### formSubmitSuccess
If the form submit is successful, make sure to call the following in addition to formSubmit()
```kotlin
NeuroID.getInstance().formSubmitSuccess()
```
```java
NeuroID.getInstance().formSubmitSuccess();
```

## Custom logging
In the event there is any interaction that needs to be logged manually, the following functions can be used
```kotlin
NeuroID.getInstance().captureEvent(
    eventName = "CUSTOM_EVENT_EXAMPLE_NAME",
    tgs = "buttonSendCustomEvent"
)
```
```java
NeuroID.getInstance().captureEvent(
    eventName = "CUSTOM_EVENT_EXAMPLE_NAME",
    tgs = "buttonSendCustomEvent"
);
```

## Sample application
If you want to see the demo of this project deployed, just launch the application "NeuroIdAndroidExample"

## License
The Neuro-ID Mobile SDK is provided under an [MIT License](LICENSE).
