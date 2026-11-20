#!/bin/bash
# Builds HorizonHome.apk without Gradle.
# Needs: JDK 17+, Python 3, and the Android SDK (build-tools 30+ and platform android-30).
#   ANDROID_HOME   path to the Android SDK            (default: ~/Android/Sdk)
#   BUILD_TOOLS    build-tools version to use         (default: newest installed)
#   KEYSTORE       keystore to sign with              (default: ./kit.keystore, created if missing)
set -e
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
BTV="${BUILD_TOOLS:-$(ls "$SDK/build-tools" | sort -V | tail -1)}"
BT="$SDK/build-tools/$BTV"
JAR="$SDK/platforms/android-30/android.jar"
[ -f "$JAR" ] || JAR="$(ls -d "$SDK"/platforms/android-3*/android.jar | sort -V | tail -1)"
VC="${VERSION_CODE:-17}"; VN="${VERSION_NAME:-3.3}"
KS="${KEYSTORE:-kit.keystore}"; KP="${KEYSTORE_PASS:-horizonkit}"
echo "SDK: $SDK  build-tools: $BTV  platform: $JAR"
rm -rf build && mkdir -p build/res build/classes build/dex
"$BT/aapt2" compile --dir res -o build/res/res.zip
"$BT/aapt2" link -I "$JAR" -A assets --manifest AndroidManifest.xml -o build/base.apk build/res/res.zip \
    --java build/gen --min-sdk-version 26 --target-sdk-version 35 --version-code "$VC" --version-name "$VN" --auto-add-overlay
LAMBDA="$BT/core-lambda-stubs.jar"
javac -nowarn -Xlint:-options -source 8 -target 8 -encoding UTF-8 -bootclasspath "$JAR" -classpath "$JAR:$LAMBDA" \
    -d build/classes $(find src build/gen -name '*.java')
python3 strip_mp.py build/classes
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib "$JAR" --output build/dex $(find build/classes -name '*.class')
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q -j ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass "$KP" -keypass "$KP" -alias horizon \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Horizon Home Kit" >/dev/null 2>&1
fi
java -jar "$BT/lib/apksigner.jar" sign --ks "$KS" --ks-pass "pass:$KP" --key-pass "pass:$KP" \
    --out build/HorizonHome.apk build/aligned.apk
java -jar "$BT/lib/apksigner.jar" verify build/HorizonHome.apk
ls -la build/HorizonHome.apk
