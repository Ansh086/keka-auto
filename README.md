# Keka Auto

Open-source Android helper for Keka clock-in and clock-out automation.

What it does:
- schedules clock-in and clock-out using Android alarms
- prompts you to unlock when the action is due
- uses an AccessibilityService to open Keka and tap the needed controls
- avoids fixed `sleep()` delays in the automation path

## Project Layout

- `app/src/main/java/com/keka/helper/` contains the Android source
- `app/src/main/res/` contains the UI and accessibility config
- `releases/` is reserved for release notes and source-only release packaging

## Build

After generating the Gradle wrapper, build with:

```powershell
.\gradlew assembleDebug
```

## Phone Setup

1. Install the APK built from this project.
2. Open the app and set your clock-in / clock-out times.
3. Grant notification permission if prompted.
4. Enable the `Keka Auto` AccessibilityService in Android settings.
5. Keep the official Keka app installed normally.

## Notes

- This repo intentionally contains helper-app source only.
- No modded Keka APK is included here.
- The code is licensed under MIT so it stays easy to inspect, fork, and improve.
