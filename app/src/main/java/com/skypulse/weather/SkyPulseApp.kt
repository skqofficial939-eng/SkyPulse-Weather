package com.skypulse.weather

import android.app.Application
import android.content.Context
import com.skypulse.weather.data.local.SkyPulseDatabase
import com.skypulse.weather.data.prefs.SettingsStore
import com.skypulse.weather.data.remote.buildOpenMeteoApi
import com.skypulse.weather.domain.NotifiedStore
import com.skypulse.weather.data.repo.OpenMeteoPlaceRepository
import com.skypulse.weather.data.repo.OpenMeteoWeatherRepository
import com.skypulse.weather.data.repo.PlaceRepository
import com.skypulse.weather.data.repo.RoomNotifiedStore
import com.skypulse.weather.data.repo.WeatherRepository
import com.skypulse.weather.notify.Notifier
import com.skypulse.weather.notify.Scheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Simple manual dependency container. Production code always uses the real Open-Meteo repositories. */
class AppContainer(context: Context) {
    val db: SkyPulseDatabase = SkyPulseDatabase.create(context)
    private val api = buildOpenMeteoApi()
    val weather: WeatherRepository = OpenMeteoWeatherRepository(api, db)
    val places: PlaceRepository = OpenMeteoPlaceRepository(api, db)
    val settings = SettingsStore(context)
    val notified: NotifiedStore = RoomNotifiedStore(db)
}

class SkyPulseApp : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannels(this)
        // Keep the background schedule in sync with the settings that affect it.
        appScope.launch {
            container.settings.settings
                .map { Triple(it.notificationsEnabled, it.refreshHours, it.enabledCategories) }
                .distinctUntilChanged()
                .collect { Scheduler.apply(this@SkyPulseApp, container.settings.current()) }
        }
    }
}
