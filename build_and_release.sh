#!/bin/bash
set -e

echo "Installing imagemagick..."
sudo apt-get install -y imagemagick > /dev/null 2>&1 || true

ICON=$(ls /home/ubuntu/.gemini/antigravity-cli/brain/4551ce3b-e6f8-4e11-aeca-92e50bff24a2/localwatch_icon_*.jpg | head -n 1)
echo "Converting icons and removing webp duplicates..."

for dir in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    rm -f /home/ubuntu/LocalWatch/android/app/src/main/res/mipmap-${dir}/*.webp
    rm -f /home/ubuntu/LocalWatch/android/app/src/main/res/mipmap-${dir}/*.png
    convert "$ICON" -resize 192x192 "/home/ubuntu/LocalWatch/android/app/src/main/res/mipmap-${dir}/ic_launcher.png" || true
    convert "$ICON" -resize 192x192 "/home/ubuntu/LocalWatch/android/app/src/main/res/mipmap-${dir}/ic_launcher_round.png" || true
    convert "$ICON" -resize 192x192 "/home/ubuntu/LocalWatch/android/app/src/main/res/mipmap-${dir}/ic_launcher_foreground.png" || true
    convert "$ICON" -resize 192x192 "/home/ubuntu/LocalWatch/android/app/src/main/res/mipmap-${dir}/ic_launcher_background.png" || true
    convert "$ICON" -resize 192x192 "/home/ubuntu/LocalWatch/android/app/src/main/res/mipmap-${dir}/ic_launcher_monochrome.png" || true
done

echo "Cleaning up CMake and Build caches manually..."
rm -rf /home/ubuntu/LocalWatch/android/app/build
rm -rf /home/ubuntu/LocalWatch/android/app/.cxx

echo "Rebuilding APKs..."
export ANDROID_HOME=$HOME/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/tools/bin:$ANDROID_HOME/platform-tools

cd /home/ubuntu/LocalWatch/android
./gradlew assembleRelease

echo "Uploading to GitHub..."
cd /home/ubuntu/LocalWatch
gh release create v1.0.2 android/app/build/outputs/apk/release/*.apk -t "LocalWatch MVP (Fixed & Premium Icon)" -n "Fixed the Force Close issue and added a premium app icon."

echo "All done!"
