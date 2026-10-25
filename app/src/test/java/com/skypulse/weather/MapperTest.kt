package com.skypulse.weather

import com.skypulse.weather.data.remote.AirCurrentDto
import com.skypulse.weather.data.remote.AirQualityDto
import com.skypulse.weather.data.remote.CurrentDto
import com.skypulse.weather.data.remote.DailyDto
import com.skypulse.weather.data.remote.ForecastDto
import com.skypulse.weather.data.remote.GeoResultDto
import com.skypulse.weather.data.remote.HourlyDto
import com.skypulse.weather.data.remote.toPlace
import com.skypulse.weather.data.remote.toSnapshot
import com.skypulse.weather.domain.AppError
import com.skypulse.weather.domain.WeatherException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MapperTest {
    private val cur = CurrentDto(time = "2026-10-24T18:00", temperature = 28.4, apparent = 30.1, humidity = 41, isDay = 1,
        precipitation = 0.0, weatherCode = 1, pressure = 1008.2, windSpeed = 12.0, windDirection = 270, windGusts = 25.0)

    @Test fun forecastIsMappedAndSortedEvenIfApiOrderIsShuffled() {
        val dto = ForecastDto(
            utcOffsetSeconds = 18000, current = cur,
            hourly = HourlyDto(
                time = listOf("2026-10-24T20:00", "2026-10-24T18:00", "2026-10-24T19:00"),
                temperature = listOf(26.0, 28.0, 27.0), precipProb = listOf(10, 0, 5),
            ),
            daily = DailyDto(
                time = listOf("2026-10-26", "2026-10-24", "2026-10-25"),
                tempMax = listOf(29.0, 31.0, 30.0), tempMin = listOf(17.0, 19.0, 18.0),
                sunrise = listOf("2026-10-26T06:22", "2026-10-24T06:20", "2026-10-25T06:21"),
            ),
        )
        val s = dto.toSnapshot(7L, 123L, null)
        assertEquals(listOf(18, 19, 20), s.hourly.map { it.time.hour })
        assertEquals(listOf(28.0, 27.0, 26.0), s.hourly.map { it.tempC })
        assertEquals(listOf(0, 5, 10), s.hourly.map { it.precipProb })
        assertEquals(listOf(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 26)), s.daily.map { it.date })
        assertEquals(listOf(31.0, 30.0, 29.0), s.daily.map { it.maxC })
        assertEquals(LocalDateTime.of(2026, 10, 24, 6, 20), s.daily.first().sunrise)
        assertEquals(18000, s.utcOffsetSeconds)
        assertEquals(7L, s.placeId)
        assertFalse(s.fromCache)
    }
    @Test fun missingFieldsStayNullInsteadOfBecomingZero() {
        val dto = ForecastDto(current = CurrentDto(time = "2026-10-24T18:00"), hourly = HourlyDto(time = listOf("2026-10-24T18:00")))
        val s = dto.toSnapshot(1L, 0L, null)
        assertNull(s.current.tempC); assertNull(s.current.humidity); assertNull(s.current.windKmh)
        assertNull(s.hourly.single().precipProb); assertNull(s.hourly.single().tempC)
        assertTrue(s.daily.isEmpty())
    }
    @Test fun shorterArraysDoNotCrash() {
        val dto = ForecastDto(current = cur, hourly = HourlyDto(time = listOf("2026-10-24T18:00", "2026-10-24T19:00"), temperature = listOf(20.0)))
        val s = dto.toSnapshot(1L, 0L, null)
        assertEquals(2, s.hourly.size); assertNull(s.hourly[1].tempC)
    }
    @Test fun unreadableTimestampsAreSkippedOrRejectedSafely() {
        val dto = ForecastDto(current = cur, hourly = HourlyDto(time = listOf("garbage", "2026-10-24T19:00")))
        assertEquals(1, dto.toSnapshot(1L, 0L, null).hourly.size)
        try { ForecastDto(current = null).toSnapshot(1L, 0L, null); throw AssertionError("expected failure") }
        catch (e: WeatherException) { assertEquals(AppError.BAD_DATA, e.kind) }
        try { ForecastDto(current = CurrentDto(time = "nope")).toSnapshot(1L, 0L, null); throw AssertionError("expected failure") }
        catch (e: WeatherException) { assertEquals(AppError.BAD_DATA, e.kind) }
    }
    @Test fun airQualityKeepsBothScalesSeparate() {
        val air = AirQualityDto(AirCurrentDto(usAqi = 155, euAqi = 62, pm25 = 60.2, pm10 = 90.0))
        val a = ForecastDto(current = cur).toSnapshot(1L, 0L, air).air!!
        assertEquals(155, a.usAqi); assertEquals(62, a.euAqi)
        assertNull(ForecastDto(current = cur).toSnapshot(1L, 0L, null).air)
    }
    @Test fun geocodingResultsKeepRegionAndCountryToDisambiguate() {
        val pk = GeoResultDto(1176615L, "Hyderabad", 25.39, 68.37, "Pakistan", "PK", "Sindh", "Asia/Karachi").toPlace()
        val inn = GeoResultDto(1269843L, "Hyderabad", 17.38, 78.47, "India", "IN", "Telangana", "Asia/Kolkata").toPlace()
        assertEquals("Hyderabad, Sindh, Pakistan", pk.label)
        assertEquals("Hyderabad, Telangana, India", inn.label)
        assertTrue(pk.label != inn.label)
        assertEquals("Okara, Punjab, Pakistan", GeoResultDto(1L, "Okara", 30.81, 73.45, "Pakistan", "PK", "Punjab").toPlace().label)
        assertEquals("Singapore", GeoResultDto(2L, "Singapore", 1.29, 103.85, null, null, "Singapore").toPlace().label)
        assertNotNull(pk.timezone)
    }
}
