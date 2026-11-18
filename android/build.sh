#!/bin/sh
# Builds now-playing.apk with the bare SDK tools, no Gradle. Needs ANDROID_HOME with platform 34 and build-tools.
set -eu
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}
BT=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
JAR=$SDK/platforms/android-34/android.jar
OUT=build
rm -rf $OUT && mkdir -p $OUT/gen $OUT/classes

"$BT/aapt2" compile --dir res -o $OUT/res.zip
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml --java $OUT/gen -o $OUT/base.apk $OUT/res.zip
javac --release 11 -classpath "$JAR" -d $OUT/classes $(find src $OUT/gen -name '*.java')
"$BT/d8" --lib "$JAR" --min-api 30 --output $OUT $(find $OUT/classes -name '*.class')
cp $OUT/base.apk $OUT/unsigned.apk
(cd $OUT && zip -q unsigned.apk classes.dex)
"$BT/zipalign" -f 4 $OUT/unsigned.apk $OUT/aligned.apk

KS=${NOWPLAYING_KEYSTORE:-$HOME/.android/nowplaying.keystore}
if [ ! -f "$KS" ]; then
    mkdir -p "$(dirname "$KS")"
    keytool -genkeypair -keystore "$KS" -storepass nowplaying -keypass nowplaying -alias nowplaying \
        -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Now Playing" >/dev/null
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:nowplaying --out now-playing.apk $OUT/aligned.apk
echo "Built android/now-playing.apk"
