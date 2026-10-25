package com.skypulse.weather.ui.forecast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skypulse.weather.domain.Day
import com.skypulse.weather.domain.Hour
import com.skypulse.weather.domain.SettingsState
import com.skypulse.weather.domain.UnitFormat
import com.skypulse.weather.domain.WeatherCodes
import com.skypulse.weather.ui.WeatherViewModel
import com.skypulse.weather.ui.components.LineChart
import com.skypulse.weather.ui.components.SkyCard
import com.skypulse.weather.ui.components.WeatherIcon
import com.skypulse.weather.ui.components.rememberReduceMotion
import com.skypulse.weather.ui.home.hourDetails
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ForecastScreen(vm: WeatherViewModel, onSearch: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val snap = st.snapshot
    val place = st.selected
    if (place == null || snap == null) {
        Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                if (place == null) "Choose a place to see its forecast." else "The forecast isn't available yet.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(12.dp))
            Button(onClick = if (place == null) onSearch else vm::refresh) { Text(if (place == null) "Search for a city" else "Try again") }
        }
        return
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val s = st.settings
    Column(Modifier.fillMaxSize()) {
        Text(
            place.label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp), maxLines = 1,
        )
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Hourly") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Daily") })
        }
        if (tab == 0) HourlyTab(snap.upcomingHours(System.currentTimeMillis(), 48), s)
        else DailyTab(snap.daily, snap.localNow(System.currentTimeMillis()).toLocalDate(), s)
    }
}

@Composable
private fun HourlyTab(hours: List<Hour>, s: SettingsState) {
    var detail by remember { mutableStateOf<Hour?>(null) }
    if (hours.isEmpty()) {
        Text("No hourly data available.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            val temps = hours.map { h -> h.tempC?.let { if (s.tempUnit == com.skypulse.weather.domain.TempUnit.C) it else UnitFormat.cToF(it) } }
            val known = temps.filterNotNull()
            SkyCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Temperature, next ${hours.size} hours", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    LineChart(
                        values = temps,
                        labels = listOf(UnitFormat.time(hours.first().time, s.use24h), UnitFormat.time(hours[hours.size / 2].time, s.use24h), UnitFormat.time(hours.last().time, s.use24h)),
                        summary = if (known.isEmpty()) "Temperature chart unavailable"
                        else "Temperature chart for the next ${hours.size} hours, from ${known.min().toInt()} to ${known.max().toInt()} degrees ${s.tempUnit.name}",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        items(hours, key = { it.time.toString() }) { h -> HourRow(h, s) { detail = h } }
    }
    detail?.let { h ->
        AlertDialog(
            onDismissRequest = { detail = null },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("Close") } },
            title = { Text("${h.time.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${UnitFormat.time(h.time, s.use24h)}") },
            text = { Text(hourDetails(h, s)) },
        )
    }
}

@Composable
private fun HourRow(h: Hour, s: SettingsState, onClick: () -> Unit) {
    val kind = WeatherCodes.kind(h.code)
    val a11y = "${h.time.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${UnitFormat.time(h.time, s.use24h)}: " +
        "${UnitFormat.temp(h.tempC, s.tempUnit, false)} degrees, ${WeatherCodes.describe(h.code)}"
    SkyCard(Modifier.clip(MaterialTheme.shapes.medium).clickable(onClickLabel = "Show details", onClick = onClick)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp).semantics(mergeDescendants = true) { contentDescription = a11y }, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(78.dp)) {
                Text(h.time.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(UnitFormat.time(h.time, s.use24h), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            WeatherIcon(kind, h.isDay, 26.dp, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("${UnitFormat.temp(h.tempC, s.tempUnit)}  ·  feels ${UnitFormat.temp(h.feelsC, s.tempUnit)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Wind ${UnitFormat.wind(h.windKmh, s.windUnit)}  ·  Humidity ${UnitFormat.percent(h.humidity)}",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(h.precipProb?.let { "$it%" } ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DailyTab(days: List<Day>, today: java.time.LocalDate, s: SettingsState) {
    val reduce = rememberReduceMotion()
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    if (days.isEmpty()) {
        Text("No daily data available.", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            val highs = days.map { d -> d.maxC?.let { if (s.tempUnit == com.skypulse.weather.domain.TempUnit.C) it else UnitFormat.cToF(it) } }
            val known = highs.filterNotNull()
            SkyCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Daily highs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    LineChart(
                        values = highs,
                        labels = listOf(days.first().date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), days.last().date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())),
                        summary = if (known.isEmpty()) "Daily highs chart unavailable"
                        else "Daily highs for ${days.size} days, from ${known.min().toInt()} to ${known.max().toInt()} degrees ${s.tempUnit.name}",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        items(days, key = { it.date.toString() }) { d ->
            val key = d.date.toString()
            val open = expanded[key] == true
            val name = if (d.date == today) "Today" else d.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            SkyCard(Modifier.clip(MaterialTheme.shapes.medium).clickable(onClickLabel = if (open) "Collapse" else "Expand") { expanded[key] = !open }) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WeatherIcon(WeatherCodes.kind(d.code), true, 28.dp, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("$name, ${d.date.dayOfMonth} ${d.date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(WeatherCodes.describe(d.code), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${UnitFormat.temp(d.maxC, s.tempUnit)} / ${UnitFormat.temp(d.minC, s.tempUnit)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    AnimatedVisibility(
                        visible = open,
                        enter = if (reduce) EnterTransition.None else expandVertically(),
                        exit = if (reduce) ExitTransition.None else shrinkVertically(),
                    ) {
                        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DetailLine("Precipitation chance", UnitFormat.percent(d.precipProbMax))
                            DetailLine("Precipitation total", UnitFormat.precip(d.precipSumMm, s.precipUnit))
                            DetailLine("Max wind", UnitFormat.wind(d.windMaxKmh, s.windUnit))
                            DetailLine("UV index (peak)", d.uvMax?.let { String.format(Locale.ENGLISH, "%.0f", it) } ?: com.skypulse.weather.domain.MISSING)
                            DetailLine("Sunrise", UnitFormat.time(d.sunrise, s.use24h))
                            DetailLine("Sunset", UnitFormat.time(d.sunset, s.use24h))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
