package com.skypulse.weather.data.remote

import com.skypulse.weather.domain.AirQuality
import com.skypulse.weather.domain.AppError
import com.skypulse.weather.domain.Current
import com.skypulse.weather.domain.Day
import com.skypulse.weather.domain.Hour
import com.skypulse.weather.domain.Place
import com.skypulse.weather.domain.WeatherException
import com.skypulse.weather.domain.WeatherSnapshot
import java.time.LocalDate
import java.time.LocalDateTime

fun GeoResultDto.toPlace() = Place(
    id = id, name = name, admin1 = admin1, country = country, countryCode = countryCode,
    latitude = latitude, longitude = longitude, timezone = timezone,
)

/** Open-Meteo returns local ISO times like "2026-10-24T18:00" when timezone=auto. */
internal fun parseLocal(s: String?): LocalDateTime? =
    s?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

fun ForecastDto.toSnapshot(placeId: Long, fetchedAtMillis: Long, air: AirQualityDto?): WeatherSnapshot {
    val c = current ?: throw WeatherException(AppError.BAD_DATA, "Forecast response has no current conditions")
    val currentTime = parseLocal(c.time)
        ?: throw WeatherException(AppError.BAD_DATA, "Forecast response has an unreadable timestamp")

    val h = hourly
    val hours = if (h == null) emptyList() else h.time.mapIndexedNotNull { i, t ->
        val time = parseLocal(t) ?: return@mapIndexedNotNull null
        Hour(
            time = time,
            tempC = h.temperature.getOrNull(i),
            feelsC = h.apparent.getOrNull(i),
            precipProb = h.precipProb.getOrNull(i),
            precipMm = h.precipitation.getOrNull(i),
            code = h.weatherCode.getOrNull(i),
            windKmh = h.windSpeed.getOrNull(i),
            humidity = h.humidity.getOrNull(i),
            uv = h.uv.getOrNull(i),
            visibilityM = h.visibility.getOrNull(i),
            dewC = h.dewPoint.getOrNull(i),
            isDay = h.isDay.getOrNull(i) == 1,
        )
    }.sortedBy { it.time }

    val d = daily
    val days = if (d == null) emptyList() else d.time.mapIndexedNotNull { i, t ->
        val date = runCatching { LocalDate.parse(t) }.getOrNull() ?: return@mapIndexedNotNull null
        Day(
            date = date,
            code = d.weatherCode.getOrNull(i),
            maxC = d.tempMax.getOrNull(i),
            minC = d.tempMin.getOrNull(i),
            sunrise = parseLocal(d.sunrise.getOrNull(i)),
            sunset = parseLocal(d.sunset.getOrNull(i)),
            uvMax = d.uvMax.getOrNull(i),
            precipSumMm = d.precipSum.getOrNull(i),
            precipProbMax = d.precipProbMax.getOrNull(i),
            windMaxKmh = d.windMax.getOrNull(i),
        )
    }.sortedBy { it.date }

    return WeatherSnapshot(
        placeId = placeId,
        utcOffsetSeconds = utcOffsetSeconds,
        fetchedAtMillis = fetchedAtMillis,
        current = Current(
            time = currentTime,
            tempC = c.temperature,
            feelsC = c.apparent,
            humidity = c.humidity,
            isDay = c.isDay == 1,
            precipMm = c.precipitation,
            code = c.weatherCode,
            pressureHpa = c.pressure,
            windKmh = c.windSpeed,
            windDirDeg = c.windDirection,
            gustKmh = c.windGusts,
        ),
        hourly = hours,
        daily = days,
        air = air?.current?.let { AirQuality(it.usAqi, it.euAqi, it.pm25, it.pm10) },
    )
}
