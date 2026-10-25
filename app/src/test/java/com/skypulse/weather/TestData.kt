package com.skypulse.weather

import com.skypulse.weather.domain.Current
import com.skypulse.weather.domain.Day
import com.skypulse.weather.domain.Hour
import com.skypulse.weather.domain.NotifiedStore
import com.skypulse.weather.domain.WeatherSnapshot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Fixed instant: 2026-10-24 13:00 UTC = 18:00 in Karachi (UTC+5). */
val NOW_MILLIS: Long = LocalDateTime.of(2026, 10, 24, 13, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
const val PK_OFFSET = 5 * 3600

class FakeStore : NotifiedStore {
    val claimed = mutableSetOf<String>()
    override suspend fun claim(key: String): Boolean = claimed.add(key)
    override suspend fun pruneOlderThan(millis: Long) {}
}

fun hour(
    time: LocalDateTime, temp: Double? = 25.0, prob: Int? = 0, code: Int? = 0, wind: Double? = 10.0,
) = Hour(time, temp, temp, prob, 0.0, code, wind, 50, 3.0, 10000.0, 12.0, time.hour in 6..18)

fun day(date: LocalDate, max: Double? = 30.0, min: Double? = 18.0, probMax: Int? = 10, uv: Double? = 5.0, code: Int? = 1) =
    Day(date, code, max, min, date.atTime(6, 20), date.atTime(17, 45), uv, 0.0, probMax, 20.0)

/** Snapshot whose local "now" is 2026-10-24 18:00 (place offset +5h). Hours run from 17:00 for 30 h. */
fun snapshot(
    hours: List<Hour>? = null,
    days: List<Day>? = null,
    offset: Int = PK_OFFSET,
    fetchedAt: Long = NOW_MILLIS,
): WeatherSnapshot {
    val start = LocalDateTime.of(2026, 10, 24, 17, 0)
    val hs = hours ?: (0 until 30).map { hour(start.plusHours(it.toLong())) }
    val today = LocalDate.of(2026, 10, 24)
    val ds = days ?: (0 until 3).map { day(today.plusDays(it.toLong())) }
    return WeatherSnapshot(
        placeId = 1L, utcOffsetSeconds = offset, fetchedAtMillis = fetchedAt,
        current = Current(LocalDateTime.of(2026, 10, 24, 18, 0), 28.0, 30.0, 40, true, 0.0, 1, 1008.0, 12.0, 270, 20.0),
        hourly = hs, daily = ds, air = null,
    )
}
