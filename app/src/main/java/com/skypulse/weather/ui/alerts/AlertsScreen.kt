package com.skypulse.weather.ui.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skypulse.weather.domain.AlertCandidate
import com.skypulse.weather.domain.AlertCategory
import com.skypulse.weather.domain.AlertRules
import com.skypulse.weather.ui.WeatherViewModel
import com.skypulse.weather.ui.components.SkyCard

@Composable
fun AlertsScreen(vm: WeatherViewModel) {
    val st by vm.state.collectAsStateWithLifecycle()
    val snap = st.snapshot
    val place = st.selected
    val advisories: List<AlertCandidate> = remember(snap, st.settings) {
        if (snap == null || place == null) emptyList()
        else AlertRules.evaluate(
            place.id, place.name, snap, st.settings, System.currentTimeMillis(),
            categories = AlertCategory.entries.filter { !it.isSummary }.toSet(),
        )
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Weather alerts", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground) }
        item {
            Text("Official warnings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        item {
            SkyCard {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("No official warning feed is connected", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.size(4.dp))
                        Text(
                            "Open-Meteo, the weather source used by SkyPulse, does not publish government warnings, and SkyPulse " +
                                "does not invent them. For official severe-weather warnings, check your national weather service " +
                                "(in Pakistan, the Pakistan Meteorological Department) or local authorities.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Advisories from the forecast${place?.let { " – ${it.name}" } ?: ""}",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (snap == null) {
            item { Text("Select a place to see advisories.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else if (advisories.isEmpty()) {
            item {
                Text(
                    "Nothing in the current forecast crosses your alert thresholds. Thresholds can be changed in Settings.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(advisories, key = { it.key }) { a ->
                SkyCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(8.dp))
                            Text(a.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.size(6.dp))
                        Text(a.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "App-generated advisory from forecast data – not an official warning.",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Background checks are scheduled by Android and can run late, especially with battery saver on. " +
                    "SkyPulse cannot promise instant alerts.",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
