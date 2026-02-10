-keep class com.neuroid.** { *; }
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }

# OkHttp platform-specific SSL implementations (optional dependencies)
# These are only used if present at runtime; OkHttp falls back to platform SSL
# still, requried for compilation
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# Google Play Services Location
# dependency for FingerprintJS, NeuroID does not use this signal but rule is required for compilation
# Only used if present at runtime for enhanced location accuracy
-dontwarn com.google.android.gms.location.CurrentLocationRequest
-dontwarn com.google.android.gms.location.CurrentLocationRequest$Builder
-dontwarn com.google.android.gms.location.FusedLocationProviderClient
-dontwarn com.google.android.gms.location.LocationServices
-dontwarn com.google.android.gms.tasks.CancellationToken
-dontwarn com.google.android.gms.tasks.OnFailureListener
-dontwarn com.google.android.gms.tasks.OnSuccessListener
-dontwarn com.google.android.gms.tasks.Task