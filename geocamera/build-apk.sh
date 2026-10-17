#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
if [[ ! -f "$ROOT/local.properties" ]]; then
  printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$ROOT/local.properties"
fi
cd "$ROOT"
./gradlew :app:testDebugUnitTest :app:assembleRelease --stacktrace
mkdir -p "$ROOT/../dist"
cp -f "$ROOT/app/build/outputs/apk/release/app-release.apk" "$ROOT/../dist/GeoCamera.apk"
echo "APK: $ROOT/../dist/GeoCamera.apk"
