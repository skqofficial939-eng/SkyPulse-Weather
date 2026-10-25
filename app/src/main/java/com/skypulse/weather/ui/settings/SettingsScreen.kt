package com.skypulse.weather.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skypulse.weather.BuildConfig
import com.skypulse.weather.data.prefs.Keys
import com.skypulse.weather.domain.AlertCategory
import com.skypulse.weather.domain.MonitorScope
import com.skypulse.weather.domain.PrecipUnit
import com.skypulse.weather.domain.PressureUnit
import com.skypulse.weather.domain.TempUnit
import com.skypulse.weather.domain.ThemeMode
import com.skypulse.weather.domain.UnitFormat
import com.skypulse.weather.domain.WindUnit
import com.skypulse.weather.notify.Notifier
import com.skypulse.weather.ui.WeatherViewModel

private fun categoryLabel(c: AlertCategory) = when (c) {
    AlertCategory.RAIN_SOON -> "Rain expected soon"
    AlertCategory.HIGH_PRECIP -> "High precipitation chance"
    AlertCategory.THUNDERSTORM -> "Thunderstorms"
    AlertCategory.HEAT -> "High temperature"
    AlertCategory.COLD -> "Low temperature"
    AlertCategory.WIND -> "Strong winds"
    AlertCategory.UV -> "High UV index"
    AlertCategory.MORNING_SUMMARY -> "Morning summary"
    AlertCategory.EVENING_SUMMARY -> "Evening summary (tomorrow)"
    AlertCategory.SIGNIFICANT_CHANGE -> "Significant weather change"
}

@Composable
fun SettingsScreen(vm: WeatherViewModel) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val ctx = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var placesMenu by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.update { set(Keys.NOTIF_ON, granted) }
        if (!granted) vm.say("Notification permission denied, so alerts stay off. You can allow it in system settings.")
    }

    fun requestEnableNotifications() {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) showRationale = true else vm.update { set(Keys.NOTIF_ON, true) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) }

        item { Header("Appearance") }
        item { ChoiceRow("Theme", ThemeMode.entries, s.theme, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { vm.update { setTheme(it) } } }

        item { Header("Units") }
        item { ChoiceRow("Temperature", TempUnit.entries, s.tempUnit, { "°${it.name}" }) { vm.update { setTemp(it) } } }
        item { ChoiceRow("Wind speed", WindUnit.entries, s.windUnit, { if (it == WindUnit.KMH) "km/h" else "mph" }) { vm.update { setWind(it) } } }
        item { ChoiceRow("Pressure", PressureUnit.entries, s.pressureUnit, { if (it == PressureUnit.HPA) "hPa" else "inHg" }) { vm.update { setPressure(it) } } }
        item { ChoiceRow("Precipitation", PrecipUnit.entries, s.precipUnit, { if (it == PrecipUnit.MM) "mm" else "in" }) { vm.update { setPrecip(it) } } }
        item { ChoiceRow("Time format", listOf(false, true), s.use24h, { if (it) "24-hour" else "12-hour" }) { vm.update { set(Keys.H24, it) } } }

        item { Header("Location") }
        item {
            Column(Modifier.padding(vertical = 6.dp)) {
                Text("Default location (shown when the app opens)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Box {
                    OutlinedButton(onClick = { placesMenu = true }, enabled = st.places.isNotEmpty()) {
                        Text(st.selected?.label ?: "No saved places yet")
                    }
                    DropdownMenu(expanded = placesMenu, onDismissRequest = { placesMenu = false }) {
                        st.places.forEach { p ->
                            DropdownMenuItem(text = { Text(p.label) }, onClick = { vm.select(p.id); placesMenu = false })
                        }
                    }
                }
                Text(
                    "Add, remove, favourite and reorder places in the Saved tab.",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { Header("Notifications") }
        item {
            SwitchRow("Weather notifications", "Master switch for all alerts and summaries below.", s.notificationsEnabled) { on ->
                if (on) requestEnableNotifications() else vm.update { set(Keys.NOTIF_ON, false) }
            }
        }
        if (s.notificationsEnabled && !Notifier.canNotify(ctx)) {
            item {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("Notifications are blocked in Android settings, so none will appear.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }) { Text("Open notification settings") }
                }
            }
        }
        if (s.notificationsEnabled) {
            items(AlertCategory.entries.toList()) { cat ->
                SwitchRow(categoryLabel(cat), null, cat in s.enabledCategories) { on -> vm.update { setCategory(cat, on) } }
            }
            item {
                Text(
                    "Official severe-weather warnings: not available. No official alert feed is connected (see the Alerts tab).",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            item { Header("Alert thresholds") }
            item { StepperRow("Rain chance to alert", "${s.rainProbPct}%", { vm.update { set(Keys.RAIN_PROB, (s.rainProbPct - 10).coerceAtLeast(10)) } }, { vm.update { set(Keys.RAIN_PROB, (s.rainProbPct + 10).coerceAtMost(100)) } }) }
            item { StepperRow("Rain look-ahead window", "${s.rainWindowHours} h", { vm.update { set(Keys.RAIN_WINDOW, (s.rainWindowHours - 1).coerceAtLeast(1)) } }, { vm.update { set(Keys.RAIN_WINDOW, (s.rainWindowHours + 1).coerceAtMost(12)) } }) }
            item { StepperRow("High precipitation chance", "${s.highPrecipPct}%", { vm.update { set(Keys.HIGH_PRECIP, (s.highPrecipPct - 10).coerceAtLeast(30)) } }, { vm.update { set(Keys.HIGH_PRECIP, (s.highPrecipPct + 10).coerceAtMost(100)) } }) }
            item { StepperRow("High temperature from", "${UnitFormat.thresholdTemp(s.heatC, s.tempUnit)}°${s.tempUnit.name}", { vm.update { set(Keys.HEAT, (s.heatC - 1).coerceAtLeast(25)) } }, { vm.update { set(Keys.HEAT, (s.heatC + 1).coerceAtMost(55)) } }) }
            item { StepperRow("Low temperature at or below", "${UnitFormat.thresholdTemp(s.coldC, s.tempUnit)}°${s.tempUnit.name}", { vm.update { set(Keys.COLD, (s.coldC - 1).coerceAtLeast(-30)) } }, { vm.update { set(Keys.COLD, (s.coldC + 1).coerceAtMost(20)) } }) }
            item { StepperRow("Strong wind from", UnitFormat.wind(s.windKmh.toDouble(), s.windUnit), { vm.update { set(Keys.WIND_T, (s.windKmh - 5).coerceAtLeast(20)) } }, { vm.update { set(Keys.WIND_T, (s.windKmh + 5).coerceAtMost(120)) } }) }
            item { StepperRow("High UV from index", "${s.uvIndex}", { vm.update { set(Keys.UV_T, (s.uvIndex - 1).coerceAtLeast(3)) } }, { vm.update { set(Keys.UV_T, (s.uvIndex + 1).coerceAtMost(11)) } }) }

            item { Header("Summaries & quiet hours") }
            item { TimeRow("Morning summary time", s.morningMin, s.use24h) { m -> vm.update { set(Keys.MORNING, m) } } }
            item { TimeRow("Evening summary time", s.eveningMin, s.use24h) { m -> vm.update { set(Keys.EVENING, m) } } }
            item { SwitchRow("Quiet hours", "No notifications are sent during this window, including summaries.", s.quietEnabled) { on -> vm.update { set(Keys.QUIET_ON, on) } } }
            if (s.quietEnabled) {
                item { TimeRow("Quiet hours start", s.quietStartMin, s.use24h) { m -> vm.update { set(Keys.QUIET_START, m) } } }
                item { TimeRow("Quiet hours end", s.quietEndMin, s.use24h) { m -> vm.update { set(Keys.QUIET_END, m) } } }
            }

            item { Header("Monitoring") }
            item {
                ChoiceRow(
                    "Locations to monitor", MonitorScope.entries, s.monitorScope,
                    { when (it) { MonitorScope.SELECTED_ONLY -> "Current place"; MonitorScope.FAVORITES -> "Favourites"; MonitorScope.ALL_SAVED -> "All saved" } },
                ) { vm.update { setScope(it) } }
            }
            item {
                ChoiceRow("Background check interval", listOf(1, 3, 6, 12), s.refreshHours, { "${it} h" }) { vm.update { set(Keys.REFRESH_H, it) } }
            }
            item {
                Text(
                    "Android decides when background checks actually run, so alerts can arrive late, particularly with battery saver on. " +
                        "The forecast also refreshes when you open the app (if data is over 15 minutes old) or pull down to refresh.",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            item { OutlinedButton(onClick = vm::sendTestNotification, modifier = Modifier.padding(top = 8.dp)) { Text("Send a test notification") } }
        }

        item { Header("Data") }
        item { OutlinedButton(onClick = vm::clearCache) { Text("Clear cached weather") } }
        item { OutlinedButton(onClick = vm::clearRecents, modifier = Modifier.padding(top = 8.dp)) { Text("Clear recent searches") } }

        item { Header("About") }
        item {
          Column {
            Text(
                "SkyPulse Weather ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE})",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Weather, geocoding and air-quality data by Open-Meteo.com, licensed CC BY 4.0.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
          }
        }
        item { TextButton(onClick = { dialog = "privacy" }) { Text("Privacy policy") } }
        item { TextButton(onClick = { dialog = "licenses" }) { Text("Open-source licenses & data attribution") } }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Allow notifications?") },
            text = { Text("SkyPulse uses notifications to tell you about rain, storms, extreme temperatures and daily summaries for the places you choose. You control every category and can set quiet hours.") },
            confirmButton = {
                TextButton(onClick = { showRationale = false; permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showRationale = false }) { Text("Not now") } },
        )
    }
    dialog?.let { which ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(if (which == "privacy") "Privacy policy" else "Licenses & attribution") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(if (which == "privacy") PRIVACY_TEXT else LICENSES_TEXT, style = MaterialTheme.typography.bodyMedium) } },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun Header(text: String) {
    Column(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics(mergeDescendants = true) {
            contentDescription = "$title, ${if (checked) "on" else "off"}"
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> ChoiceRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { o ->
                FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) })
            }
        }
    }
}

@Composable
private fun StepperRow(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        OutlinedButton(onClick = onMinus, modifier = Modifier.semantics { contentDescription = "Decrease $title" }) { Text("−") }
        Text(value, Modifier.padding(horizontal = 10.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        OutlinedButton(onClick = onPlus, modifier = Modifier.semantics { contentDescription = "Increase $title" }) { Text("+") }
    }
}

@Composable
private fun TimeRow(title: String, minutes: Int, use24h: Boolean, onPick: (Int) -> Unit) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        OutlinedButton(onClick = {
            TimePickerDialog(ctx, { _, h, m -> onPick(h * 60 + m) }, minutes / 60, minutes % 60, use24h).show()
        }) { Text(UnitFormat.minutesToClock(minutes, use24h)) }
    }
}

private const val PRIVACY_TEXT = """SkyPulse Weather has no account, no ads and no analytics.

What stays on your phone: your saved places, settings, recent searches and cached forecasts are stored locally and are never uploaded by SkyPulse.

What is sent over the internet:
• Place searches (the text you type) go to the Open-Meteo geocoding service.
• To show weather, the latitude and longitude of the place you are viewing go to Open-Meteo's forecast and air-quality services. Open-Meteo, like any web service, can see your IP address.

Your current location: only if you tap "Use my location". SkyPulse then asks Android for approximate location once, while the app is open. The coordinates are used to fetch weather and to look up a place name using Android's built-in geocoder (which may contact Google's location services on your device). Coordinates are saved on your phone as your "current location" place and are not logged. SkyPulse never tracks your location in the background; background notification checks use the places you saved.

Notifications: generated on your phone from forecast data. Alerts labelled as advisories are computed by the app and are not official warnings.

You can delete cached data and recent searches in Settings, remove saved places in the Saved tab, or uninstall the app to remove everything."""

private const val LICENSES_TEXT = """Weather data: Open-Meteo.com, licensed under CC BY 4.0 (https://open-meteo.com). The free Open-Meteo API is intended for non-commercial use.

Open-source libraries (Apache License 2.0 unless noted):
• AndroidX: Core, Activity, Lifecycle, Navigation, Room, DataStore, WorkManager
• Jetpack Compose and Material 3
• Kotlin, kotlinx.coroutines, kotlinx.serialization
• Retrofit, OkHttp and the Retrofit kotlinx-serialization converter
• Google Play services Location (subject to the Android SDK / Google Play services terms)

Full license texts are available at the projects' websites."""
