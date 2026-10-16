# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Uncomment to remove logging from release builds.
#-assumenosideeffects class android.util.Log {
#    public static boolean isLoggable(java.lang.String, int);
#    public static int v(...);
#    public static int d(...);
#    public static int i(...);
#    public static int e(...);
#    public static int w(...);
#}

-keeppackagenames com.neuroid.**
-keepattributes Signature
-keepattributes *Annotation*

-keep class com.google.gson.** { *; }

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keepclassmembers class * {
    @com.google.gson.annotations.Expose <fields>;
}

-keepclassmembers class * {
    @com.google.gson.annotations.Since <fields>;
}

-keepclassmembers class * {
    @com.google.gson.annotations.Until <fields>;
}

# have to keep these classes
-keep class com.neuroid.tracker.models.** { *; }
-keep class com.neuroid.tracker.events.** { *; }
-keep interface com.neuroid.tracker.NeuroIDPublic { *; }
-keep class com.neuroid.tracker.NeuroID { *; }
-keep class com.neuroid.tracker.utils.NIDMetaData { *; }
-keep interface com.neuroid.tracker.compose.JetpackCompose { *; }
-keep class com.neuroid.tracker.NeuroID$Companion { *; }
-keep class com.neuroid.tracker.NeuroID$Builder { *; }
-keep class com.neuroid.tracker.NeuroID$BuilderConfig { *; }
-keep class com.neuroid.tracker.extensions.** { *; }

# Kotlin generates NeuroIDPublic$DefaultImpls to dispatch calls to interface methods that
# have default parameter values (e.g. start(), startSession(), startAppFlow()). Consumers
# calling through the NeuroIDPublic interface type with an omitted default argument resolve
# to this synthetic class at their own compile/runtime; it is not reachable from this
# module's own call graph, so R8 strips it unless explicitly kept.
-keep class com.neuroid.tracker.NeuroIDPublic$DefaultImpls { *; }

# Need to keep this class because of the following runtime crash (keeps the jetpack compose
# implementation class from being stripped out by R8):
# FATAL EXCEPTION: main
# Process: com.neuroid.example, PID: 4689
# java.lang.NoSuchMethodError: No virtual method getCompose()Lcom/neuroid/tracker/compose/JetpackComposeImpl;
# in class Lcom/neuroid/tracker/NeuroID$Companion; or its super classes (declaration of 'com.neuroid.tracker.NeuroID$Companion'
# appears in /data/app/~~BPV0XrGplx9HViq5naGv_Q==/com.neuroid.example-tmNUkE82nBr5AtjRPsYs0g==/base.apk!classes3.dex)
-keep class com.neuroid.tracker.compose.JetpackComposeImpl { *; }
