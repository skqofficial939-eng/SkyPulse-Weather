# SkyPulse Weather

Native Android weather app: Kotlin, Jetpack Compose (Material 3), MVVM, Room, DataStore, WorkManager,
Retrofit/OkHttp, kotlinx.serialization. Tagline: *Every Change in the Sky, Informed.*

Weather, worldwide place search and air quality come from **Open-Meteo** (no API key needed).
Package: `com.skypulse.weather` · minSdk 26 · compile/target SDK 35.

## Honest build status

| Item | Status |
|---|---|
| Source project (this folder) | Complete |
| Pure-logic unit tests (units, alert rules, dedupe, quiet hours, time zones, cache expiry, DTO mapping) | **46/46 passed** when compiled with `kotlinc` and run with a small stand-in runner (not Gradle/JUnit) |
| Gradle build, Android/Compose/Room/Retrofit code | **Never compiled.** The authoring environment had no Android SDK and blocked Google Maven / Maven Central |
| APK | **Not built yet.** Build it with the GitHub Actions workflow below |
| Live Open-Meteo calls (Okara, Lahore, Narowal) | **Not tested.** The authoring environment blocked `open-meteo.com` |
| `ForecastJsonTest` (JSON decoding), DataStore/Room persistence, UI tests | Written or noted but **not run** (need the real Gradle build / an Android device) |

Because the Android code was never compiled, expect the first CI run to possibly report a few compile errors
(versions in `gradle/libs.versions.toml` were not verified against the live repositories). The workflow
fails visibly and uploads test reports; send me the log and I will fix it.

## Build the APK in the cloud (no Android Studio needed)

1. Create an empty repository on github.com (e.g. `skypulse-weather`, no README).
2. In this folder:
   ```
   git init
   git add .
   git commit -m "SkyPulse Weather"
   git branch -M main
   git remote add origin https://github.com/<your-username>/skypulse-weather.git
   git push -u origin main
   ```
3. Open the repo on GitHub > **Actions** > **Build SkyPulse APK**. It starts automatically on push; you can also press **Run workflow**.
4. When the run is green, open it and download **SkyPulse-debug-apk** from *Artifacts* (a zip containing `app-debug.apk`).
   If it is red, open the failed step; the *unit-test-reports* artifact has test details.

## Install on your phone

1. Unzip the artifact and copy `app-debug.apk` to the phone (USB, Drive, or message it to yourself).
2. Tap the file. When Android asks, allow your file manager/browser to **install unknown apps**.
3. Tap **Install**, then **Open**. The debug build installs as `com.skypulse.weather.debug`, so it can sit beside a future release build.
4. Notifications: Android 13+ asks for permission when you switch them on in **Settings**.

The debug APK is signed with the standard Android debug key: fine for personal use, **not** for Play Store publishing.
A release build needs your own keystore (never commit it; `*.jks`/`keystore.properties` are git-ignored).

## Build locally (optional)

JDK 17, Android Studio (or SDK platform 35 + build-tools), then `./gradlew testDebugUnitTest assembleDebug`.
Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Features

- Home: hero with current temperature, feels-like, today's high/low, observation time, fresh-vs-saved label; next 24 h; detail cards (tap for an explanation); 10-day forecast with temperature-range bars; air quality (US and European scales shown separately); forecast-based insights.
- Forecast tab: 48-hour chart and rows, daily rows that expand to show precipitation, wind, UV, sunrise/sunset.
- Search: Open-Meteo geocoding (worldwide, region + country shown to tell similar names apart), recent searches, duplicate prevention. Pakistan names such as Okara, Lahore, Narowal are resolved by the service; **no city list is hard-coded** and support for any given village depends on the geocoder.
- Saved places: favourites, reorder, remove, filter, current-location entry.
- Optional device location: one-shot, approximate, foreground-only, only after tapping the button. Denial leaves search fully working.
- Settings: theme (system/light/dark), temperature/wind/pressure/precipitation units, 12/24 h, default place, per-category notifications, thresholds, quiet hours, summary times, places to monitor, background interval, test notification, clear cache, privacy policy, licences.
- Offline: the last forecast per place is cached in Room and shown with a "Saved data · updated …" label and a retry banner; a cache younger than 15 minutes avoids needless requests.

## Notifications: what they are and are not

- Categories: rain soon, high precipitation chance, thunderstorms, heat, cold, strong wind, UV, morning summary, evening summary, significant change. Each is a separate switch; thresholds, quiet hours and monitored places change the behaviour.
- They are **app-generated advisories computed from forecast numbers, and are labelled that way. They are not official warnings.** No official alert feed is connected: Open-Meteo does not provide government warnings, and none are invented. The Alerts tab says so. For Pakistan use the Pakistan Meteorological Department / NDMA.
- Delivery uses WorkManager (periodic, network-required, exponential backoff). Android decides when jobs run, so alerts can be late, especially under battery saver. There is no exact-time or instant delivery. Truly instant official warnings would need an authorised alert feed plus a push backend (e.g. FCM), which is out of scope here.
- Duplicates are prevented by a persisted key per event (place + day/time block), stored in Room for 3 days. Alerts suppressed by quiet hours are not consumed, so they can still be sent afterwards if still relevant.
- No background location tracking. Background checks use the places you saved (including the last "current location" fix).

## Data providers and restrictions

- Open-Meteo (forecast, geocoding, air quality): CC BY 4.0 attribution is shown in the app. Its free API is for **non-commercial** use; commercial use needs a paid plan. Terms and limits can change; check open-meteo.com.
- Reverse geocoding of the device position uses Android's built-in `Geocoder` (may contact Google services). If unavailable, the place is named "Current location".
- No API keys are used, so nothing secret is in the repo. If you add a keyed provider, keep the key out of source control and note that keys inside an APK can be extracted.

## Known limitations

- Not compiled or live-tested yet (see table above).
- No weather map/radar: no verified global radar provider was available, and a fake one would be wrong.
- No official severe-weather warnings (see above).
- Reverse geocoding quality depends on the device's Geocoder.
- Fonts are the system sans-serif (no bundled font).
- Reduced-motion support follows Android's "remove animations" setting.
- Time/zone logic uses the UTC offset returned by Open-Meteo for each forecast, so a cached forecast keeps the offset it was fetched with.
- Quiet hours and summary times use the phone's clock, not the forecast place's.
- Release builds are unsigned until you add a keystore; minification is off.

## Regenerate the icons

The launcher icon, round icon, adaptive-icon foreground and in-app logo are all generated from `assets/SkyPulse_logo.png` (unaltered original):
`pip install pillow && python3 tools/make_icons.py`.
The supplied artwork contains no embedded text label.

See `docs/TEST_PLAN.md` for the manual test checklist.
