package com.skypulse.weather.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap as snapSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skypulse.weather.R
import com.skypulse.weather.domain.Day
import com.skypulse.weather.domain.Hour
import com.skypulse.weather.domain.Insights
import com.skypulse.weather.domain.MISSING
import com.skypulse.weather.domain.Place
import com.skypulse.weather.domain.SettingsState
import com.skypulse.weather.domain.UnitFormat
import com.skypulse.weather.domain.WeatherCodes
import com.skypulse.weather.domain.WeatherException
import com.skypulse.weather.domain.WeatherSnapshot
import com.skypulse.weather.ui.WeatherViewModel
import com.skypulse.weather.ui.components.HomeSkeleton
import com.skypulse.weather.ui.components.InfoBanner
import com.skypulse.weather.ui.components.SectionTitle
import com.skypulse.weather.ui.components.SkyCard
import com.skypulse.weather.ui.components.WeatherIcon
import com.skypulse.weather.ui.components.rememberReduceMotion
import com.skypulse.weather.ui.rememberLocationAction
import com.skypulse.weather.ui.relativeAge
import com.skypulse.weather.ui.theme.Sky
import com.skypulse.weather.ui.theme.skyBrush
import java.time.Duration
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: WeatherViewModel, onSearch: () -> Unit, onOpenSaved: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    if (!st.loaded) { HomeSkeleton(); return }
    val place = st.selected
    if (place == null) { WelcomeScreen(vm, onSearch, st.locating); return }
    val snap = st.snapshot
    PullToRefreshBox(
        isRefreshing = st.loading && snap != null,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            snap == null && st.error != null -> ErrorState(st.error!!, onRetry = vm::refresh, onSearch = onSearch)
            snap == null -> HomeSkeleton()
            else -> HomeContent(place, snap, st.settings, st.error, st.loading, vm, onSearch, onOpenSaved)
        }
    }
}

@Composable
private fun WelcomeScreen(vm: WeatherViewModel, onSearch: () -> Unit, locating: Boolean) {
    val useLocation = rememberLocationAction(vm)
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.skypulse_logo), contentDescription = "SkyPulse logo", modifier = Modifier.size(112.dp).clip(RoundedCornerShape(26.dp)))
        Spacer(Modifier.height(20.dp))
        Text("SkyPulse Weather", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Every Change in the Sky, Informed.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = useLocation, enabled = !locating, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (locating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else { Icon(Icons.Filled.MyLocation, null); Spacer(Modifier.width(8.dp)); Text("Use my location") }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSearch, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.Search, null); Spacer(Modifier.width(8.dp)); Text("Search for a city")
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Location is optional. If you tap “Use my location”, SkyPulse asks once for approximate location, " +
                "only while the app is open, to find the weather where you are. You can always search for any city instead.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorState(error: WeatherException, onRetry: () -> Unit, onSearch: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.CloudOff, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(error.message ?: "Couldn't load the forecast.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Try again") }
        TextButton(onClick = onSearch) { Text("Choose another place") }
    }
}

private data class Metric(val title: String, val value: String, val detail: String?, val icon: ImageVector, val explanation: String)

@Composable
private fun HomeContent(
    place: Place,
    snap: WeatherSnapshot,
    s: SettingsState,
    error: WeatherException?,
    loading: Boolean,
    vm: WeatherViewModel,
    onSearch: () -> Unit,
    onOpenSaved: () -> Unit,
) {
    val now = remember(snap) { System.currentTimeMillis() }
    val reduce = rememberReduceMotion()
    var metricDialog by remember { mutableStateOf<Metric?>(null) }
    var hourDialog by remember { mutableStateOf<Hour?>(null) }
    val today = snap.localNow(now).toLocalDate()
    val todayDay = snap.daily.firstOrNull { it.date == today } ?: snap.daily.firstOrNull()
    val hours = remember(snap) { snap.upcomingHours(now, 24) }
    val currentHour = hours.firstOrNull()
    val metrics = remember(snap, s) { buildMetrics(snap, todayDay, currentHour, s) }
    val insights = remember(snap, s) { Insights.generate(snap, s, now) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.skypulse_logo), contentDescription = "SkyPulse", modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(place.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                    val region = place.shortRegion
                    if (region.isNotEmpty()) Text(region, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                IconButton(onClick = onSearch) { Icon(Icons.Filled.Search, "Search for a place", tint = MaterialTheme.colorScheme.onBackground) }
                IconButton(onClick = onOpenSaved) { Icon(Icons.Filled.Bookmarks, "Saved locations", tint = MaterialTheme.colorScheme.onBackground) }
            }
        }
        if (error != null) {
            item {
                InfoBanner(
                    "${error.message} Showing saved data from ${relativeAge(snap.fetchedAtMillis, now)}.",
                    action = { TextButton(onClick = vm::refresh) { Text("Retry") } },
                )
            }
        }
        item {
            Crossfade(targetState = snap.placeId, animationSpec = if (reduce) snapSpec() else tween(350), label = "hero") {
                Hero(snap, todayDay, s, now)
            }
        }
        if (hours.isNotEmpty()) {
            item { SectionTitle("Next 24 hours") }
            item {
                SkyCard {
                    LazyRow(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(hours, key = { it.time.toString() }) { h ->
                            HourChip(h, isNow = h === hours.first(), s) { hourDialog = h }
                        }
                    }
                }
            }
        }
        if (metrics.isNotEmpty()) {
            item { SectionTitle("Details") }
            items(metrics.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { m -> MetricCard(m, Modifier.weight(1f)) { metricDialog = m } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (snap.daily.isNotEmpty()) {
            item { SectionTitle("${snap.daily.size}-day forecast") }
            item { DailyCompact(snap.daily, today, s) }
        }
        snap.air?.let { air ->
            if (air.usAqi != null || air.euAqi != null || air.pm25 != null) {
                item { SectionTitle("Air quality") }
                item { AirCard(air.usAqi, air.euAqi, air.pm25, air.pm10) }
            }
        }
        if (insights.isNotEmpty()) {
            item { SectionTitle("Insights") }
            item {
                SkyCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        insights.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) }
                    }
                }
            }
        }
        item {
            Text(
                "Weather data by Open-Meteo.com (CC BY 4.0). Forecasts are model estimates and can change.",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    metricDialog?.let { m ->
        AlertDialog(
            onDismissRequest = { metricDialog = null },
            confirmButton = { TextButton(onClick = { metricDialog = null }) { Text("Close") } },
            title = { Text("${m.title}: ${m.value}") },
            text = { Text(m.explanation) },
        )
    }
    hourDialog?.let { h ->
        AlertDialog(
            onDismissRequest = { hourDialog = null },
            confirmButton = { TextButton(onClick = { hourDialog = null }) { Text("Close") } },
            title = { Text("${UnitFormat.time(h.time, s.use24h)} – ${WeatherCodes.describe(h.code)}") },
            text = { Text(hourDetails(h, s)) },
        )
    }
}

internal fun hourDetails(h: Hour, s: SettingsState): String = listOf(
    "Temperature: ${UnitFormat.tempWithUnit(h.tempC, s.tempUnit)}",
    "Feels like: ${UnitFormat.tempWithUnit(h.feelsC, s.tempUnit)}",
    "Precipitation chance: ${UnitFormat.percent(h.precipProb)}",
    "Precipitation: ${UnitFormat.precip(h.precipMm, s.precipUnit)}",
    "Wind: ${UnitFormat.wind(h.windKmh, s.windUnit)}",
    "Humidity: ${UnitFormat.percent(h.humidity)}",
    "UV index: ${h.uv?.let { String.format(Locale.ENGLISH, "%.1f", it) } ?: MISSING}",
    "Visibility: ${UnitFormat.visibility(h.visibilityM, s.windUnit == com.skypulse.weather.domain.WindUnit.KMH)}",
    "Dew point: ${UnitFormat.tempWithUnit(h.dewC, s.tempUnit)}",
).joinToString("\n")

@Composable
private fun Hero(snap: WeatherSnapshot, today: Day?, s: SettingsState, now: Long) {
    val c = snap.current
    val kind = WeatherCodes.kind(c.code)
    val desc = WeatherCodes.describe(c.code)
    val white = Color.White
    Column(
        Modifier.fillMaxWidth().background(skyBrush(kind, c.isDay), RoundedCornerShape(28.dp)).padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(kind, c.isDay, 30.dp, null)
            Spacer(Modifier.width(8.dp))
            Text(desc, style = MaterialTheme.typography.titleMedium, color = white)
        }
        Text(
            UnitFormat.temp(c.tempC, s.tempUnit),
            style = MaterialTheme.typography.displayLarge, color = white,
            modifier = Modifier.semantics {
                contentDescription = if (c.tempC == null) "Current temperature unavailable"
                else "Current temperature ${UnitFormat.temp(c.tempC, s.tempUnit, false)} degrees ${if (s.tempUnit == com.skypulse.weather.domain.TempUnit.C) "Celsius" else "Fahrenheit"}"
            },
        )
        Text("Feels like ${UnitFormat.temp(c.feelsC, s.tempUnit)}", style = MaterialTheme.typography.bodyLarge, color = white)
        Spacer(Modifier.height(4.dp))
        Text(
            "H ${UnitFormat.temp(today?.maxC, s.tempUnit)}   L ${UnitFormat.temp(today?.minC, s.tempUnit)}",
            style = MaterialTheme.typography.titleMedium, color = white,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Conditions as of ${UnitFormat.time(c.time, s.use24h)} local time",
            style = MaterialTheme.typography.labelMedium, color = white.copy(alpha = 0.92f),
        )
        Text(
            if (snap.fromCache) "Saved data · updated ${relativeAge(snap.fetchedAtMillis, now)}"
            else "Updated ${relativeAge(snap.fetchedAtMillis, now)}",
            style = MaterialTheme.typography.labelMedium, color = white.copy(alpha = 0.92f),
        )
    }
}

@Composable
private fun HourChip(h: Hour, isNow: Boolean, s: SettingsState, onClick: () -> Unit) {
    val label = if (isNow) "Now" else UnitFormat.hourLabel(h.time, s.use24h)
    val kind = WeatherCodes.kind(h.code)
    val a11y = "$label, ${UnitFormat.temp(h.tempC, s.tempUnit, false)} degrees, ${WeatherCodes.describe(h.code)}" +
        (h.precipProb?.let { ", $it percent chance of precipitation" } ?: "")
    Column(
        Modifier
            .width(66.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClickLabel = "Show hour details", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = a11y }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        WeatherIcon(kind, h.isDay, 28.dp, null, tint = if (kind == com.skypulse.weather.domain.WeatherKind.CLEAR && h.isDay) Sky.Sun else MaterialTheme.colorScheme.primary)
        Text(UnitFormat.temp(h.tempC, s.tempUnit), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            h.precipProb?.let { "$it%" } ?: " ",
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun buildMetrics(snap: WeatherSnapshot, today: Day?, hour: Hour?, s: SettingsState): List<Metric> {
    val c = snap.current
    val out = mutableListOf<Metric>()
    c.humidity?.let {
        out += Metric("Humidity", "$it%", null, Icons.Filled.Opacity,
            "Relative humidity is how much water vapour the air holds compared with the most it could hold at this temperature. High humidity makes warm weather feel heavier.")
    }
    c.windKmh?.let {
        val gust = c.gustKmh?.let { g -> "Gusts ${UnitFormat.wind(g, s.windUnit)}" }
        out += Metric("Wind", "${UnitFormat.wind(it, s.windUnit)} ${UnitFormat.compass(c.windDirDeg)}", gust, Icons.Filled.Air,
            "Wind speed is measured 10 m above ground. The letters show the direction the wind is blowing from.")
    }
    today?.precipProbMax?.let {
        out += Metric("Precip. chance", "$it%", "Today’s peak", Icons.Filled.WaterDrop,
            "The highest chance of measurable precipitation at any point today. It is a probability, not a guarantee.")
    }
    if (c.precipMm != null || today?.precipSumMm != null) {
        out += Metric("Rainfall", UnitFormat.precip(c.precipMm ?: today?.precipSumMm, s.precipUnit),
            today?.precipSumMm?.let { "Today: ${UnitFormat.precip(it, s.precipUnit)}" }, Icons.Filled.WaterDrop,
            "Current precipitation, with the forecast total for the whole day underneath. Includes rain, showers and melted snow.")
    }
    c.pressureHpa?.let {
        out += Metric("Pressure", UnitFormat.pressure(it, s.pressureUnit), null, Icons.Filled.Compress,
            "Sea-level air pressure. Falling pressure often signals unsettled weather; rising pressure usually means improving conditions.")
    }
    hour?.visibilityM?.let {
        out += Metric("Visibility", UnitFormat.visibility(it, s.windUnit == com.skypulse.weather.domain.WindUnit.KMH), null, Icons.Filled.Visibility,
            "How far you can see clearly. Fog, heavy rain, smoke and dust reduce it.")
    }
    today?.uvMax?.let {
        out += Metric("UV index", String.format(Locale.ENGLISH, "%.0f", it), "Today’s peak", Icons.Filled.WbSunny,
            "Strength of the sun’s ultraviolet radiation. 3–5 is moderate, 6–7 high, 8–10 very high and 11+ extreme. Use sun protection from 3 upward.")
    }
    today?.sunrise?.let {
        out += Metric("Sunrise", UnitFormat.time(it, s.use24h), null, Icons.Filled.WbTwilight, "Local time of sunrise at this place.")
    }
    val sunset = today?.sunset
    if (sunset != null) {
        val sunrise = today?.sunrise
        val len = if (sunrise != null) Duration.between(sunrise, sunset) else null
        out += Metric("Sunset", UnitFormat.time(sunset, s.use24h), len?.let { d -> "Daylight ${d.toHours()}h ${d.toMinutes() % 60}m" }, Icons.Filled.WbTwilight,
            "Local time of sunset at this place.")
    }
    hour?.dewC?.let {
        out += Metric("Dew point", UnitFormat.tempWithUnit(it, s.tempUnit), null, Icons.Filled.Thermostat,
            "The temperature at which air becomes saturated and dew forms. A dew point above about 20°C feels muggy.")
    }
    return out
}

@Composable
private fun MetricCard(m: Metric, modifier: Modifier, onClick: () -> Unit) {
    SkyCard(modifier.clip(MaterialTheme.shapes.medium).clickable(onClickLabel = "Explain ${m.title}", onClick = onClick)) {
        Column(Modifier.padding(14.dp).semantics(mergeDescendants = true) {
            contentDescription = "${m.title}: ${m.value}${m.detail?.let { ", $it" } ?: ""}. Double tap for explanation."
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(m.icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(m.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(m.value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            if (m.detail != null) Text(m.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DailyCompact(days: List<Day>, today: java.time.LocalDate, s: SettingsState) {
    val lo = days.mapNotNull { it.minC }.minOrNull()
    val hi = days.mapNotNull { it.maxC }.maxOrNull()
    SkyCard {
        Column(Modifier.padding(vertical = 6.dp)) {
            days.forEach { d ->
                val name = if (d.date == today) "Today" else d.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                val kind = WeatherCodes.kind(d.code)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$name: ${WeatherCodes.describe(d.code)}, low ${UnitFormat.temp(d.minC, s.tempUnit, false)}, " +
                                "high ${UnitFormat.temp(d.maxC, s.tempUnit, false)} degrees" +
                                (d.precipProbMax?.let { ", $it percent chance of precipitation" } ?: "")
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, Modifier.width(52.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    WeatherIcon(kind, true, 24.dp, null, tint = if (kind == com.skypulse.weather.domain.WeatherKind.CLEAR) Sky.Sun else MaterialTheme.colorScheme.primary)
                    Text(
                        d.precipProbMax?.let { "$it%" } ?: "",
                        Modifier.width(44.dp).padding(start = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                    )
                    Text(UnitFormat.temp(d.minC, s.tempUnit), Modifier.width(36.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
                    RangeBar(lo, hi, d.minC, d.maxC, Modifier.weight(1f).padding(horizontal = 8.dp))
                    Text(UnitFormat.temp(d.maxC, s.tempUnit), Modifier.width(36.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RangeBar(overallMin: Double?, overallMax: Double?, dayMin: Double?, dayMax: Double?, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.outline
    val fill = MaterialTheme.colorScheme.primary
    Canvas(modifier.height(6.dp)) {
        drawRoundRect(track, size = size, cornerRadius = CornerRadius(size.height / 2))
        if (overallMin == null || overallMax == null || dayMin == null || dayMax == null) return@Canvas
        val span = (overallMax - overallMin).takeIf { it > 0 } ?: return@Canvas
        val start = ((dayMin - overallMin) / span).toFloat().coerceIn(0f, 1f)
        val end = ((dayMax - overallMin) / span).toFloat().coerceIn(start, 1f)
        drawRoundRect(
            fill, topLeft = Offset(size.width * start, 0f),
            size = Size((size.width * (end - start)).coerceAtLeast(size.height), size.height),
            cornerRadius = CornerRadius(size.height / 2),
        )
    }
}

@Composable
private fun AirCard(usAqi: Int?, euAqi: Int?, pm25: Double?, pm10: Double?) {
    SkyCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (usAqi != null) {
                Text("US AQI $usAqi · ${UnitFormat.aqiLabelUs(usAqi)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            if (euAqi != null) {
                Text("European AQI $euAqi · ${UnitFormat.aqiLabelEu(euAqi)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            val parts = listOfNotNull(
                pm25?.let { "PM2.5 ${String.format(Locale.ENGLISH, "%.1f", it)} µg/m³" },
                pm10?.let { "PM10 ${String.format(Locale.ENGLISH, "%.1f", it)} µg/m³" },
            )
            if (parts.isNotEmpty()) Text(parts.joinToString("   "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "The US and European scales are different and are never combined. Values are modelled estimates, not station readings.",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
