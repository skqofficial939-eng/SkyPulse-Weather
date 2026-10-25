package com.skypulse.weather

import com.skypulse.weather.data.remote.AppJson
import com.skypulse.weather.data.remote.ForecastDto
import com.skypulse.weather.data.remote.GeocodingResponseDto
import com.skypulse.weather.data.remote.toPlace
import com.skypulse.weather.data.remote.toSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uses hand-written JSON in the documented Open-Meteo shape (not a captured live response). */
class ForecastJsonTest {
    private val forecastJson = """
    {"latitude":30.8,"longitude":73.45,"utc_offset_seconds":18000,"timezone":"Asia/Karachi","unknown_future_field":1,
     "current":{"time":"2026-10-24T18:00","interval":900,"temperature_2m":28.4,"relative_humidity_2m":41,"apparent_temperature":30.1,
       "is_day":1,"precipitation":0.0,"weather_code":1,"pressure_msl":1008.2,"wind_speed_10m":12.0,"wind_direction_10m":270,"wind_gusts_10m":25.0},
     "hourly":{"time":["2026-10-24T18:00","2026-10-24T19:00"],"temperature_2m":[28.4,null],"precipitation_probability":[0,15],
       "weather_code":[1,2],"is_day":[1,0],"visibility":[24140.0,20000.0]},
     "daily":{"time":["2026-10-24"],"weather_code":[1],"temperature_2m_max":[31.2],"temperature_2m_min":[19.0],
       "sunrise":["2026-10-24T06:20"],"sunset":["2026-10-24T17:45"],"uv_index_max":[6.4],"precipitation_probability_max":[15]}}
    """.trimIndent()

    @Test fun decodesRealisticForecastIgnoringUnknownKeysAndNulls() {
        val s = AppJson.decodeFromString(ForecastDto.serializer(), forecastJson).toSnapshot(1L, 0L, null)
        assertEquals(28.4, s.current.tempC!!, 0.001)
        assertEquals(2, s.hourly.size)
        assertNull(s.hourly[1].tempC)
        assertEquals(15, s.hourly[1].precipProb)
        assertTrue(s.hourly[0].isDay); assertTrue(!s.hourly[1].isDay)
        assertEquals(6.4, s.daily.single().uvMax!!, 0.001)
        assertNull(s.daily.single().windMaxKmh)
    }
    @Test fun decodesGeocodingAndHandlesEmptyResults() {
        val json = """{"results":[{"id":1,"name":"Narowal","latitude":32.1,"longitude":74.87,"country":"Pakistan","country_code":"PK","admin1":"Punjab","timezone":"Asia/Karachi"}],"generationtime_ms":0.5}"""
        assertEquals("Narowal, Punjab, Pakistan", AppJson.decodeFromString(GeocodingResponseDto.serializer(), json).results!!.single().toPlace().label)
        assertNull(AppJson.decodeFromString(GeocodingResponseDto.serializer(), """{"generationtime_ms":0.3}""").results)
    }
}
