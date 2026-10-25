package com.skypulse.weather.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.skypulse.weather.domain.AlertCategory
import com.skypulse.weather.domain.MonitorScope
import com.skypulse.weather.domain.NO_PLACE
import com.skypulse.weather.domain.PrecipUnit
import com.skypulse.weather.domain.PressureUnit
import com.skypulse.weather.domain.SettingsState
import com.skypulse.weather.domain.TempUnit
import com.skypulse.weather.domain.ThemeMode
import com.skypulse.weather.domain.WindUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "skypulse_settings")

object Keys {
    val THEME = stringPreferencesKey("theme")
    val TEMP = stringPreferencesKey("temp_unit")
    val WIND = stringPreferencesKey("wind_unit")
    val PRESSURE = stringPreferencesKey("pressure_unit")
    val PRECIP = stringPreferencesKey("precip_unit")
    val H24 = booleanPreferencesKey("use_24h")
    val SELECTED = longPreferencesKey("selected_place")
    val REFRESH_H = intPreferencesKey("refresh_hours")
    val NOTIF_ON = booleanPreferencesKey("notifications_enabled")
    val CATEGORIES = stringPreferencesKey("categories") // comma separated enum names
    val RAIN_PROB = intPreferencesKey("rain_prob")
    val RAIN_WINDOW = intPreferencesKey("rain_window")
    val HIGH_PRECIP = intPreferencesKey("high_precip")
    val HEAT = intPreferencesKey("heat_c")
    val COLD = intPreferencesKey("cold_c")
    val WIND_T = intPreferencesKey("wind_kmh")
    val UV_T = intPreferencesKey("uv_index")
    val QUIET_ON = booleanPreferencesKey("quiet_on")
    val QUIET_START = intPreferencesKey("quiet_start")
    val QUIET_END = intPreferencesKey("quiet_end")
    val MORNING = intPreferencesKey("morning_min")
    val EVENING = intPreferencesKey("evening_min")
    val SCOPE = stringPreferencesKey("monitor_scope")
}

private inline fun <reified E : Enum<E>> Preferences.enumOr(key: Preferences.Key<String>, default: E): E =
    this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

fun Preferences.toSettings(): SettingsState {
    val d = SettingsState()
    val cats = this[Keys.CATEGORIES]?.split(',')?.filter { it.isNotBlank() }
        ?.mapNotNull { n -> AlertCategory.entries.firstOrNull { it.name == n } }?.toSet()
    return SettingsState(
        theme = enumOr(Keys.THEME, d.theme),
        tempUnit = enumOr(Keys.TEMP, d.tempUnit),
        windUnit = enumOr(Keys.WIND, d.windUnit),
        pressureUnit = enumOr(Keys.PRESSURE, d.pressureUnit),
        precipUnit = enumOr(Keys.PRECIP, d.precipUnit),
        use24h = this[Keys.H24] ?: d.use24h,
        selectedPlaceId = this[Keys.SELECTED] ?: NO_PLACE,
        refreshHours = this[Keys.REFRESH_H] ?: d.refreshHours,
        notificationsEnabled = this[Keys.NOTIF_ON] ?: d.notificationsEnabled,
        enabledCategories = cats ?: d.enabledCategories,
        rainProbPct = this[Keys.RAIN_PROB] ?: d.rainProbPct,
        rainWindowHours = this[Keys.RAIN_WINDOW] ?: d.rainWindowHours,
        highPrecipPct = this[Keys.HIGH_PRECIP] ?: d.highPrecipPct,
        heatC = this[Keys.HEAT] ?: d.heatC,
        coldC = this[Keys.COLD] ?: d.coldC,
        windKmh = this[Keys.WIND_T] ?: d.windKmh,
        uvIndex = this[Keys.UV_T] ?: d.uvIndex,
        quietEnabled = this[Keys.QUIET_ON] ?: d.quietEnabled,
        quietStartMin = this[Keys.QUIET_START] ?: d.quietStartMin,
        quietEndMin = this[Keys.QUIET_END] ?: d.quietEndMin,
        morningMin = this[Keys.MORNING] ?: d.morningMin,
        eveningMin = this[Keys.EVENING] ?: d.eveningMin,
        monitorScope = enumOr(Keys.SCOPE, d.monitorScope),
    )
}

class SettingsStore(private val context: Context) {
    val settings: Flow<SettingsState> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): SettingsState = settings.first()

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setCategory(category: AlertCategory, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val now = prefs.toSettings().enabledCategories.toMutableSet()
            if (enabled) now.add(category) else now.remove(category)
            prefs[Keys.CATEGORIES] = now.joinToString(",") { it.name }
        }
    }

    suspend fun setTheme(v: ThemeMode) = set(Keys.THEME, v.name)
    suspend fun setTemp(v: TempUnit) = set(Keys.TEMP, v.name)
    suspend fun setWind(v: WindUnit) = set(Keys.WIND, v.name)
    suspend fun setPressure(v: PressureUnit) = set(Keys.PRESSURE, v.name)
    suspend fun setPrecip(v: PrecipUnit) = set(Keys.PRECIP, v.name)
    suspend fun setScope(v: MonitorScope) = set(Keys.SCOPE, v.name)
    suspend fun setSelectedPlace(id: Long) = set(Keys.SELECTED, id)
}
