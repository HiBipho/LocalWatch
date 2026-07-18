#!/bin/bash
set -e

echo "Cleaning up CMake and Build caches manually..."
rm -rf /home/ubuntu/LocalWatch/android/app/build
rm -rf /home/ubuntu/LocalWatch/android/app/.cxx

echo "Rebuilding APKs..."
export ANDROID_HOME=$HOME/android-sdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/tools/bin:$ANDROID_HOME/platform-tools

cd /home/ubuntu/LocalWatch/android
./gradlew assembleRelease

echo "Committing fixes to git..."
cd /home/ubuntu/LocalWatch
git add .
git commit -m "fix: Disable New Architecture for legacy native module support" || true
git push origin main || true

echo "Uploading to GitHub..."
gh release create v1.0.4-debug android/app/build/outputs/apk/release/*.apk -t "LocalWatch MVP (Debug Version)" -n "Injected a Global Error Catcher. If the app crashes, it will show an alert box instead of force closing."

echo "All done!"
