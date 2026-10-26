# FireHome

Apple TV-style apps screen for Fire TV. It opens like a normal app (Fire OS keeps the Home button).

## Build
Push this folder to a new GitHub repo (branch `main`). The Actions workflow builds the APK and attaches it to the `latest` release.

## Install on the stick
    wget https://github.com/<you>/<repo>/releases/download/latest/app-debug.apk
    adb connect <fire-tv-ip>:5555
    adb install -r app-debug.apk
    adb shell monkey -p dev.firehome -c android.intent.category.LEANBACK_LAUNCHER 1

Afterwards find "Apps" in Your Apps & Channels (See all).

## Customize
Edit `DOCK` (the four dock apps, by package name) and `HIDDEN` at the top of `MainActivity.java`.
