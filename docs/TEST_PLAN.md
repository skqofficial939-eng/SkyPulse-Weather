# SkyPulse manual test plan

Run on a real phone with the debug APK. "Automated" = covered by `./gradlew testDebugUnitTest`.

| # | Scenario | Steps | Expected | Automated coverage |
|---|---|---|---|---|
| 1 | First launch | Fresh install, open | Welcome screen with "Use my location" and "Search for a city"; no weather shown | - |
| 2 | Manual city search | Search tab > type "Lah" | Suggestions appear after ~0.35 s with region and country | DTO to place labels |
| 3 | Location permission granted | Welcome > Use my location > Allow | Current place added and selected; forecast loads | - |
| 4 | Location permission denied | Same, choose Don't allow | Message shown; search still works | - |
| 5 | Okara | Search "Okara", pick Okara, Punjab, Pakistan | Current, hourly, daily data load | label test (not live) |
| 6 | Lahore | As above | Same | - |
| 7 | Narowal | As above | Same (if the geocoder lacks it, a "no places found" message appears) | - |
| 8 | Non-Pakistan city | Search "Toronto" | Local time and units correct for that city | time-zone tests |
| 9 | Hourly forecast | Home next-24h row, Forecast > Hourly | Hours start at the place's current local hour; tap shows details | upcomingHours test |
| 10 | Daily forecast | Home 10-day list, Forecast > Daily | Rows expand with sun times, UV, wind | ordering test |
| 11 | Switch saved cities | Places tab > tap another city | Home switches with fade; favourite, reorder, delete work | - |
| 12 | Dark/light | Settings > Theme | All screens readable in both; status bar icons stay visible | - |
| 13 | Notification permission granted | Settings > switch on > Continue > Allow; send test | Test notification appears; tap opens app | - |
| 14 | Permission denied | Same, choose Don't allow | Switch stays off, explanatory message | - |
| 15 | Duplicate alert prevention | Trigger same alert twice (lower rain threshold) | Only one notification per event | dispatcher tests |
| 16 | Offline start | Airplane mode, open app | Last forecast shown with "Saved data" label and Retry banner | cache policy tests |
| 17 | Failed request | Block network mid-refresh | Error banner, no crash, cached data kept | error mapping (manual) |
| 18 | Restart/persistence | Change units/theme/places, force-stop, reopen | Everything restored | needs instrumented test (not written) |
| 19 | Units | Switch C/F, km/h-mph, hPa-inHg, mm-in | Every screen updates | unit tests |
| 20 | Quiet hours | Set window around now, send alert-worthy data | Nothing delivered; delivered after window if still relevant | dispatcher tests |
