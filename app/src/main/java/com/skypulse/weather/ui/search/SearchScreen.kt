package com.skypulse.weather.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skypulse.weather.ui.SearchViewModel
import com.skypulse.weather.ui.WeatherViewModel

@Composable
fun SearchScreen(vm: WeatherViewModel, svm: SearchViewModel, onBack: () -> Unit, onPicked: () -> Unit) {
    val query by svm.query.collectAsStateWithLifecycle()
    val ui by svm.ui.collectAsStateWithLifecycle()
    val recents by svm.recents.collectAsStateWithLifecycle()
    val home by vm.state.collectAsStateWithLifecycle()
    val savedIds = remember(home.places) { home.places.map { it.id }.toSet() }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            OutlinedTextField(
                value = query,
                onValueChange = svm::setQuery,
                modifier = Modifier.weight(1f).focusRequester(focus),
                singleLine = true,
                placeholder = { Text("Search city, town or region") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { svm.setQuery("") }) { Icon(Icons.Filled.Close, "Clear search") }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                shape = MaterialTheme.shapes.large,
            )
        }

        val trimmed = query.trim()
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (trimmed.length < 2) {
                if (recents.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent searches", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                            TextButton(onClick = vm::clearRecents) { Text("Clear") }
                        }
                    }
                    items(recents) { r ->
                        Row(
                            Modifier.fillMaxWidth().clickable { svm.setQuery(r) }.padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.size(12.dp))
                            Text(r, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                } else {
                    item {
                        Text(
                            "Type at least two letters to search cities, towns and regions worldwide.",
                            Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                if (ui.loading) item { Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
                ui.error?.let { err ->
                    item {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(err.message ?: "Search failed.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.size(8.dp))
                            Button(onClick = svm::retry) { Text("Retry") }
                        }
                    }
                }
                if (!ui.loading && ui.error == null && ui.searchedQuery == trimmed && ui.results.isEmpty()) {
                    item {
                        Text(
                            "No places found for “$trimmed”. Check the spelling, or try a nearby larger town or the district name.",
                            Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(ui.results, key = { it.id }) { place ->
                    val saved = place.id in savedIds
                    Row(
                        Modifier.fillMaxWidth().clickable(onClickLabel = "Show weather for ${place.label}") {
                            svm.remember(trimmed)
                            vm.addAndSelect(place)
                            onPicked()
                        }.padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(place.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text(
                                listOfNotNull(place.admin1?.takeIf { it != place.name }, place.country).joinToString(", ") +
                                    (place.countryCode?.let { " ($it)" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (saved) Text("Saved", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
