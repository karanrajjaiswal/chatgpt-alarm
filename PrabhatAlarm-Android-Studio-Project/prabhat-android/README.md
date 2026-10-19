# Prabhat Alarm — Android project

A standalone native Android alarm-clock project with a modern dark interface and a location-aware Brahma Muhurta scheduler. The UI and wake-up interaction are original; the general idea of configurable wake-up challenges is inspired by the Alarmy feature set referenced by the requester. This project is not affiliated with Alarmy or its publisher.

## Features included

- **Brahma Muhurta alarm:** calculates local sunrise on-device and defines the window as 96 to 48 minutes before sunrise. The default alarm is the midpoint, 72 minutes before sunrise; the offset is adjustable from 48 to 96 minutes.
- **Daily sunrise recalculation:** re-plans after app launch, reboot, device date/time changes, and time-zone changes. When location permission has been granted, launching the app refreshes its location in the foreground.
- **Regular daily alarms:** add, edit, enable/disable, and delete everyday alarms.
- **Wake-up challenge:** a simple arithmetic challenge is required to dismiss the ringing screen; an optional five-minute snooze is available.
- **Ringing service and high-priority alarm notification:** uses the system alarm ringtone and a full-screen notification intent to support lock-screen presentation.
- **Privacy-first location:** requests foreground coarse/fine location only. The last saved coordinates remain on the device. The app does not send location to a server or require an account.
- **Modern interface:** home dashboard, Brahma timing detail, settings, sunrise card, and dark lavender/saffron/mint styling.

## Build and install

### Requirements

- Android Studio that supports **Android SDK 36**
- JDK 17 or newer
- Internet access on the development computer for the first Gradle/dependency download
- Android device or emulator running Android 8.0 (API 26) or newer

### Build a debug APK

1. Open this `prabhat-android` folder in Android Studio.
2. Allow Gradle sync and install Android SDK 36 if prompted.
3. Choose **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. Android Studio will create `app/build/outputs/apk/debug/app-debug.apk`.
5. Copy that APK to your Android device and install it. Android may ask you to allow installation from the app used to open the file.

Command-line build on macOS/Linux:

```bash
./gradlew assembleDebug
```

Windows:

```bat
gradlew.bat assembleDebug
```

The included Gradle bootstrap downloads Gradle 8.13 on its first run; Android Gradle Plugin and Android dependencies are then downloaded by Gradle. You can also use Android Studio's embedded JDK.


## Build an APK automatically with GitHub

This repository includes `.github/workflows/build-apk.yml`. After you push the project to a GitHub repository, GitHub Actions builds the debug APK automatically on pushes to `main`/`master`; you can also run it manually from the **Actions** tab using **Build Android APK → Run workflow**.

When the workflow finishes successfully:

1. Open your GitHub repository and choose **Actions**.
2. Select the latest **Build Android APK** run.
3. In **Artifacts**, download `PrabhatAlarm-debug-apk`.
4. Extract the downloaded artifact ZIP. It contains `app-debug.apk`; transfer that APK to your Android phone and install it.

The first build can take several minutes while Android SDK packages and dependencies download. This creates a **debug APK for personal testing**, not a signed release suitable for Play Store publishing.

## First-run setup

1. Allow notifications when prompted.
2. Open **Settings → Refresh current location** (or tap **Use current location** on the Brahma card) and allow approximate or precise location.
3. Enable **Brahma Muhurta**. If location is unavailable, regular alarms still work.
4. On Android 14+, open **Settings → Open Android alarm display settings** and allow full-screen alarm display if the system presents that option.
5. Set device alarm volume to a suitable level. Some manufacturers also have battery optimization or auto-start settings that may affect alarms after the app is force-stopped.

The app declares `USE_EXACT_ALARM` because precise alarms are its core clock feature. Android/Google Play policy may restrict distribution of apps using this permission; review current policy before publishing publicly.

## Timing method and limitations

The app uses the NOAA-style sunrise equation (including the 90.833° sunrise zenith correction) to estimate sunrise. Its calculated window is:

- Start: sunrise − 96 minutes
- End: sunrise − 48 minutes
- Default alarm: sunrise − 72 minutes

This is a common traditional convention for Brahma Muhurta, not a single universally enforced definition. Sunrise calculations can differ from observed local sunrise due to terrain, atmospheric conditions, and equation approximations. Extreme polar locations where sunrise does not occur on a date are not supported by this simple calculator.

To respect location privacy and avoid continuous tracking, the app does **not** request background-location permission. It recalculates each day from the most recently saved coordinates and refreshes the location while the app is open. For a new location, open the app and refresh location. The system may supply a newer cached fix after a date/time or reboot event, but fully automatic background GPS updates are intentionally not enabled.

Exact alarms and full-screen alarm screens depend on Android permissions, notifications being enabled, device settings, and manufacturer-specific background restrictions. The project source is provided for Android Studio; this workspace did not have the Android SDK or dependency network access available, so an APK could not be compiled and device-tested in this environment.

## Project layout

- `app/src/main/java/com/prabhat/alarm/PrabhatUi.kt` — Compose dashboard and settings UI
- `AlarmScheduler.kt` — exact daily alarm scheduling
- `SolarCalculator.kt` — local sunrise and Brahma Muhurta calculation
- `AlarmReceiver.kt` / `SystemEventReceiver.kt` — alarm, date/time, and reboot events
- `AlarmRingingService.kt` / `RingActivity.kt` — alarm audio, full-screen notification, and math challenge
- `AlarmModel.kt` — local preference storage

## References

- Alarmy feature reference from Google Play: https://play.google.com/store/apps/details?id=droom.sleepIfUCan&hl=en_IN
- Android alarm scheduling guidance: https://developer.android.com/develop/background-work/services/alarms
- Android exact-alarm permission guidance: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- NOAA sunrise/sunset equations: https://gml.noaa.gov/grad/solcalc/solareqns.PDF
