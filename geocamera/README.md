# NT™ Geo Camera

Light-themed camera for Android and iPhone. Photos are stamped with time, GPS, accuracy, altitude, heading, address, and a site note. Each photo can also carry client details, a mobile number, and a landmark with the north, east, south, and west sides plus GPS corners walked around the boundary. Video records at the highest quality the phone offers, up to 4K. The gallery builds one PDF with four photos on each page and the details under every photo.

The camera opens as soon as the app starts. There is no account, password, or hosting website.

## Install the Android APK

`dist/GeoCamera.apk` is the signed release, version 1.3.0. Android 8 or newer. Allow camera, microphone, and location. Use **Add client, mobile, and landmark** before a shot. Mark a corner while standing on each boundary point.

## iPhone

Open `ios/NTGeoCamera.xcodeproj` on a Mac with Xcode 15 or newer, choose your signing team, and run it on an iPhone. This Linux build produces the Android APK only; an iPhone install file has to be made by Xcode.

## Rebuild the APK

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=$HOME/android-sdk
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

The sideload keystore is `release.keystore` (alias `geocamera`, store and key password `geocamera`). Replace it before any Play Store upload.
