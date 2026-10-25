package com.skypulse.weather.ui.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skypulse.weather.domain.Place
import com.skypulse.weather.ui.WeatherViewModel
import com.skypulse.weather.ui.components.SkyCard
import com.skypulse.weather.ui.rememberLocationAction

@Composable
fun SavedScreen(vm: WeatherViewModel, onAdd: () -> Unit, onOpen: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val useLocation = rememberLocationAction(vm)
    var filter by rememberSaveable { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<Place?>(null) }
    val filtering = filter.isNotBlank()
    val shown = remember(st.places, filter) {
        if (!filtering) st.places
        else st.places.filter { it.label.contains(filter.trim(), ignoreCase = true) }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Saved locations", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            item {
                OutlinedButton(onClick = useLocation, enabled = !st.locating, modifier = Modifier.fillMaxWidth()) {
                    if (st.locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else { Icon(Icons.Filled.MyLocation, null); Spacer(Modifier.size(8.dp)); Text("Use my current location") }
                }
            }
            if (st.places.size > 3) {
                item {
                    OutlinedTextField(
                        value = filter, onValueChange = { filter = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Filter saved places") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = { if (filter.isNotEmpty()) IconButton(onClick = { filter = "" }) { Icon(Icons.Filled.Close, "Clear filter") } },
                        keyboardOptions = KeyboardOptions.Default,
                        shape = MaterialTheme.shapes.large,
                    )
                }
            }
            if (st.places.isEmpty()) {
                item {
                    Text(
                        "No saved places yet. Tap + to search for a city, or use your current location.",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else if (shown.isEmpty()) {
                item { Text("No saved place matches “${filter.trim()}”.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(shown, key = { it.id }) { place ->
                val selected = place.id == st.selected?.id
                val index = st.places.indexOfFirst { it.id == place.id }
                SkyCard(Modifier.clip(MaterialTheme.shapes.medium).clickable(onClickLabel = "Show weather for ${place.name}") { vm.select(place.id); onOpen() }) {
                    Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text(place.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            val sub = if (place.isCurrentLocation) "Current location" else place.shortRegion
                            if (sub.isNotEmpty() || selected) {
                                Text(
                                    listOf(sub.takeIf { it.isNotEmpty() }, if (selected) "Showing now" else null).filterNotNull().joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { vm.setFavorite(place.id, !place.isFavorite) }) {
                            Icon(
                                if (place.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                if (place.isFavorite) "Remove ${place.name} from favourites" else "Add ${place.name} to favourites",
                                tint = if (place.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!filtering) {
                            IconButton(onClick = { vm.move(place.id, true) }, enabled = index > 0) { Icon(Icons.Filled.KeyboardArrowUp, "Move ${place.name} up") }
                            IconButton(onClick = { vm.move(place.id, false) }, enabled = index < st.places.lastIndex) { Icon(Icons.Filled.KeyboardArrowDown, "Move ${place.name} down") }
                        }
                        IconButton(onClick = { toDelete = place }) { Icon(Icons.Filled.Delete, "Remove ${place.name}") }
                    }
                }
            }
        }
        FloatingActionButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Filled.Add, "Add a place")
        }
    }

    toDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Remove ${p.name}?") },
            text = { Text("It will be removed from your saved locations and its cached forecast deleted.") },
            confirmButton = { TextButton(onClick = { vm.remove(p.id); toDelete = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}
