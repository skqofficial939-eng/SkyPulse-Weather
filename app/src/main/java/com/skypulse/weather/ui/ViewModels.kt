package com.skypulse.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.skypulse.weather.AppContainer
import com.skypulse.weather.SkyPulseApp
import com.skypulse.weather.data.prefs.SettingsStore
import com.skypulse.weather.domain.AppError
import com.skypulse.weather.domain.CURRENT_LOCATION_ID
import com.skypulse.weather.domain.Place
import com.skypulse.weather.domain.SettingsState
import com.skypulse.weather.domain.WeatherException
import com.skypulse.weather.domain.WeatherSnapshot
import com.skypulse.weather.notify.Notifier
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val places: List<Place> = emptyList(),
    val selected: Place? = null,
    val snapshot: WeatherSnapshot? = null,
    val loading: Boolean = false,
    val error: WeatherException? = null,
    val settings: SettingsState = SettingsState(),
    val loaded: Boolean = false,
    val locating: Boolean = false,
)

@Suppress("UNCHECKED_CAST")
fun factoryOf(create: () -> ViewModel) = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}

class WeatherViewModel(private val app: SkyPulseApp) : ViewModel() {
    private val c: AppContainer = app.container
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            combine(c.places.places, c.settings.settings) { p, s -> p to s }.collect { (places, s) ->
                val sel = places.firstOrNull { it.id == s.selectedPlaceId } ?: places.firstOrNull()
                val prev = _state.value.selected
                val changed = sel?.id != prev?.id || sel?.latitude != prev?.latitude || sel?.longitude != prev?.longitude
                _state.update { it.copy(places = places, selected = sel, settings = s, loaded = true) }
                if (changed) loadFor(sel, force = false)
            }
        }
    }

    private fun loadFor(place: Place?, force: Boolean) {
        loadJob?.cancel()
        if (place == null) {
            _state.update { it.copy(snapshot = null, loading = false, error = null) }
            return
        }
        loadJob = viewModelScope.launch {
            val existing = _state.value.snapshot?.takeIf { it.placeId == place.id }
            val shown = existing ?: c.weather.cached(place.id)
            _state.update { it.copy(snapshot = shown, loading = true, error = null) }
            c.weather.refresh(place, force).fold(
                onSuccess = { snap -> _state.update { it.copy(snapshot = snap, loading = false, error = null) } },
                onFailure = { e ->
                    val err = e as? WeatherException ?: WeatherException(AppError.UNKNOWN, "Something went wrong.", e)
                    // Keep whatever cached data we have, but mark it as cached so it is never shown as live.
                    _state.update { it.copy(snapshot = it.snapshot?.copy(fromCache = true), loading = false, error = err) }
                },
            )
        }
    }

    fun refresh() = loadFor(_state.value.selected, force = true)

    /** Called when the app returns to the foreground; the repository skips the network if the cache is still fresh. */
    fun refreshIfStale() {
        val st = _state.value
        val sel = st.selected ?: return
        if (st.loading) return
        loadFor(sel, force = false)
    }

    fun select(placeId: Long) {
        viewModelScope.launch { c.settings.setSelectedPlace(placeId) }
    }

    /** Adds a searched place (duplicates are ignored) and makes it the selected one. */
    fun addAndSelect(place: Place) {
        viewModelScope.launch {
            val added = c.places.add(place)
            c.settings.setSelectedPlace(place.id)
            if (!added) _messages.tryEmit("${place.name} is already in your saved locations.")
        }
    }

    fun remove(placeId: Long) {
        viewModelScope.launch {
            c.places.remove(placeId)
            if (_state.value.settings.selectedPlaceId == placeId) {
                val next = c.places.all().firstOrNull()
                c.settings.setSelectedPlace(next?.id ?: com.skypulse.weather.domain.NO_PLACE)
            }
        }
    }

    fun setFavorite(id: Long, fav: Boolean) { viewModelScope.launch { c.places.setFavorite(id, fav) } }
    fun move(id: Long, up: Boolean) { viewModelScope.launch { c.places.move(id, up) } }

    fun useMyLocation() {
        if (_state.value.locating) return
        _state.update { it.copy(locating = true) }
        viewModelScope.launch {
            val coords = LocationHelper.currentCoordinates(app)
            if (coords == null) {
                _messages.tryEmit("Couldn't get your location. Check that location is on, or search for a city.")
            } else {
                val place = LocationHelper.toPlace(app, coords.first, coords.second)
                c.places.saveCurrentLocation(place)
                c.settings.setSelectedPlace(CURRENT_LOCATION_ID)
            }
            _state.update { it.copy(locating = false) }
        }
    }

    fun locationDenied() {
        _messages.tryEmit("Location permission denied. You can still search for any city.")
    }

    fun update(block: suspend SettingsStore.() -> Unit) {
        viewModelScope.launch { c.settings.block() }
    }

    fun sendTestNotification() {
        val sel = _state.value.selected
        val ok = Notifier.showTest(app, sel?.id ?: com.skypulse.weather.domain.NO_PLACE, sel?.name)
        _messages.tryEmit(
            if (ok) "Test notification sent." else "Notifications are blocked. Allow them in system settings to receive alerts.",
        )
    }

    fun clearCache() {
        viewModelScope.launch {
            c.weather.clearCache()
            _state.update { it.copy(snapshot = null) }
            _messages.tryEmit("Cached weather cleared.")
            loadFor(_state.value.selected, force = true)
        }
    }

    fun clearRecents() { viewModelScope.launch { c.places.clearRecents() } }

    fun say(message: String) { _messages.tryEmit(message) }
}

data class SearchUi(
    val loading: Boolean = false,
    val results: List<Place> = emptyList(),
    val error: WeatherException? = null,
    val searchedQuery: String = "",
)

@OptIn(FlowPreview::class)
class SearchViewModel(private val c: AppContainer) : ViewModel() {
    val query = MutableStateFlow("")
    private val _ui = MutableStateFlow(SearchUi())
    val ui: StateFlow<SearchUi> = _ui.asStateFlow()
    val recents: StateFlow<List<String>> =
        c.places.recentSearches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            query.debounce(350).map { it.trim() }.distinctUntilChanged().collectLatest { runSearch(it) }
        }
    }

    private suspend fun runSearch(q: String) {
        if (q.length < 2) { _ui.value = SearchUi(); return }
        _ui.update { it.copy(loading = true, error = null) }
        c.places.search(q).fold(
            onSuccess = { _ui.value = SearchUi(results = it, searchedQuery = q) },
            onFailure = { _ui.value = SearchUi(error = it as? WeatherException, searchedQuery = q) },
        )
    }

    fun retry() { viewModelScope.launch { runSearch(query.value.trim()) } }
    fun setQuery(q: String) { query.value = q }
    fun remember(q: String) { viewModelScope.launch { c.places.rememberSearch(q) } }
}
