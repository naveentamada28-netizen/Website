# NT™ Geo Camera

Light-themed camera for Android and iPhone. Photos are stamped with time, GPS, accuracy, altitude, heading, address, and a site note. Video records at the highest quality the phone offers, up to 4K. The gallery builds one PDF with four photos on each page and the details under every photo.

Sign-in uses a mobile number and password kept on your hosting website. The site stores a password hash, not the readable password.

## Install the Android APK

`dist/GeoCamera.apk` is the signed release. Android 8 or newer. On first launch, enter the website address (the folder that contains `index.php`), then create an account or log in. Allow camera, microphone, and location.

## Hosting website

Upload the `hosting` folder to PHP hosting (PHP 8+). Open `https://your-domain/hosting/` to create an account. Point both apps at that same address. Account files are written to `hosting/data/`, which is blocked from web downloads.

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
