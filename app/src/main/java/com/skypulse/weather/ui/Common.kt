package com.skypulse.weather.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Returns a click handler that asks for foreground (coarse) location only when invoked, i.e. when the
 * user explicitly taps "Use my location". Denial is handled gracefully: manual search still works.
 */
@Composable
fun rememberLocationAction(vm: WeatherViewModel): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.useMyLocation() else vm.locationDenied()
    }
    return remember(vm, ctx) {
        {
            val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) vm.useMyLocation() else launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
}

/** "just now", "12 min ago", "3 h ago", or a date for anything older than a day. */
fun relativeAge(fetchedAtMillis: Long, nowMillis: Long): String {
    val mins = ((nowMillis - fetchedAtMillis) / 60_000L).coerceAtLeast(0)
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "$mins min ago"
        mins < 24 * 60 -> "${mins / 60} h ago"
        else -> DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault())
            .format(Instant.ofEpochMilli(fetchedAtMillis).atZone(ZoneId.systemDefault()))
    }
}
