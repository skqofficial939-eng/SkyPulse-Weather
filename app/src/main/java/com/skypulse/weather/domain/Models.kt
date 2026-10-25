package com.skypulse.weather.domain

import java.time.LocalDate
import java.time.LocalDateTime

/** Id reserved for the single "current device location" entry. Open-Meteo place ids are always positive. */
const val CURRENT_LOCATION_ID = -1L
const val NO_PLACE = -2L

data class Place(
    val id: Long,
    val name: String,
    val admin1: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0,
) {
    val isCurrentLocation: Boolean get() = id == CURRENT_LOCATION_ID

    /** "Okara, Punjab, Pakistan" - disambiguates similarly named cities. */
    val label: String
        get() = listOfNotNull(name, admin1?.takeIf { it != name }, country).joinToString(", ")

    val shortRegion: String
        get() = listOfNotNull(admin1?.takeIf { it != name }, countryCode ?: country).joinToString(", ")
}

/** Every measurement is nullable on purpose: "not provided" must never be shown as 0. */
data class Current(
    val time: LocalDateTime,
    val tempC: Double?,
    val feelsC: Double?,
    val humidity: Int?,
    val isDay: Boolean,
    val precipMm: Double?,
    val code: Int?,
    val pressureHpa: Double?,
    val windKmh: Double?,
    val windDirDeg: Int?,
    val gustKmh: Double?,
)

data class Hour(
    val time: LocalDateTime,
    val tempC: Double?,
    val feelsC: Double?,
    val precipProb: Int?,
    val precipMm: Double?,
    val code: Int?,
    val windKmh: Double?,
    val humidity: Int?,
    val uv: Double?,
    val visibilityM: Double?,
    val dewC: Double?,
    val isDay: Boolean,
)

data class Day(
    val date: LocalDate,
    val code: Int?,
    val maxC: Double?,
    val minC: Double?,
    val sunrise: LocalDateTime?,
    val sunset: LocalDateTime?,
    val uvMax: Double?,
    val precipSumMm: Double?,
    val precipProbMax: Int?,
    val windMaxKmh: Double?,
)

data class AirQuality(
    val usAqi: Int?,
    val euAqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
)

data class WeatherSnapshot(
    val placeId: Long,
    val utcOffsetSeconds: Int,
    val fetchedAtMillis: Long,
    val current: Current,
    val hourly: List<Hour>,
    val daily: List<Day>,
    val air: AirQuality?,
    /** True when this came from the local cache because a refresh failed or was not attempted. */
    val fromCache: Boolean = false,
) {
    /** Wall-clock time at the place right now (not the device's zone). */
    fun localNow(nowMillis: Long): LocalDateTime =
        LocalDateTime.ofEpochSecond(Math.floorDiv(nowMillis, 1000L) + utcOffsetSeconds, 0, java.time.ZoneOffset.UTC)

    /** Hours starting from the current local hour. */
    fun upcomingHours(nowMillis: Long, count: Int): List<Hour> {
        val now = localNow(nowMillis).withMinute(0).withSecond(0).withNano(0)
        return hourly.filter { !it.time.isBefore(now) }.take(count)
    }
}

enum class AppError { OFFLINE, RATE_LIMITED, SERVER, BAD_DATA, NOT_FOUND, UNKNOWN }

class WeatherException(val kind: AppError, message: String, cause: Throwable? = null) : Exception(message, cause)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class TempUnit { C, F }
enum class WindUnit { KMH, MPH }
enum class PressureUnit { HPA, INHG }
enum class PrecipUnit { MM, IN }
enum class MonitorScope { SELECTED_ONLY, FAVORITES, ALL_SAVED }

enum class AlertCategory(val isSummary: Boolean = false) {
    RAIN_SOON, HIGH_PRECIP, THUNDERSTORM, HEAT, COLD, WIND, UV, MORNING_SUMMARY(true), EVENING_SUMMARY(true), SIGNIFICANT_CHANGE
}

data class SettingsState(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val tempUnit: TempUnit = TempUnit.C,
    val windUnit: WindUnit = WindUnit.KMH,
    val pressureUnit: PressureUnit = PressureUnit.HPA,
    val precipUnit: PrecipUnit = PrecipUnit.MM,
    val use24h: Boolean = false,
    val selectedPlaceId: Long = NO_PLACE,
    val refreshHours: Int = 3,
    val notificationsEnabled: Boolean = false,
    val enabledCategories: Set<AlertCategory> = setOf(AlertCategory.RAIN_SOON, AlertCategory.THUNDERSTORM),
    val rainProbPct: Int = 60,
    val rainWindowHours: Int = 3,
    val highPrecipPct: Int = 80,
    val heatC: Int = 38,
    val coldC: Int = 5,
    val windKmh: Int = 50,
    val uvIndex: Int = 8,
    val quietEnabled: Boolean = true,
    val quietStartMin: Int = 22 * 60,
    val quietEndMin: Int = 7 * 60,
    val morningMin: Int = 7 * 60 + 30,
    val eveningMin: Int = 19 * 60,
    val monitorScope: MonitorScope = MonitorScope.SELECTED_ONLY,
)
