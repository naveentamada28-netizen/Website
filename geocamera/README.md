# Geo Camera

Phone camera that stamps the time, GPS coordinates, accuracy, altitude, compass heading, address, and an optional site note onto each photo. The same coordinates are written into the JPEG GPS tags and the file is saved to `Pictures/GeoCamera`.

## Install the APK

`dist/GeoCamera.apk` is a signed release build. Copy it to an Android phone (Android 8 or newer), open it, and allow install from that source. On first launch, allow camera and location.

## Rebuild

Requires JDK 17 and Android SDK 35.

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=$HOME/android-sdk
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

The sideload keystore is `release.keystore` (alias `geocamera`, store and key password `geocamera`). Replace it before any Play Store upload.
