package com.skypulse.weather

import com.skypulse.weather.domain.MISSING
import com.skypulse.weather.domain.PrecipUnit
import com.skypulse.weather.domain.PressureUnit
import com.skypulse.weather.domain.TempUnit
import com.skypulse.weather.domain.UnitFormat
import com.skypulse.weather.domain.WindUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

class UnitFormatTest {
    @Test fun celsiusToFahrenheit() {
        assertEquals("32°", UnitFormat.temp(0.0, TempUnit.F))
        assertEquals("212°", UnitFormat.temp(100.0, TempUnit.F))
        assertEquals("-40°", UnitFormat.temp(-40.0, TempUnit.F))
        assertEquals("86°", UnitFormat.temp(30.0, TempUnit.F))
    }
    @Test fun temperatureFormattingRoundsAndNeverShowsNegativeZero() {
        assertEquals("28°", UnitFormat.temp(27.6, TempUnit.C))
        assertEquals("0°", UnitFormat.temp(-0.3, TempUnit.C))
        assertEquals("30°C", UnitFormat.tempWithUnit(30.2, TempUnit.C))
        assertEquals("28", UnitFormat.temp(28.0, TempUnit.C, withSymbol = false))
    }
    @Test fun missingValuesAreNotZero() {
        assertEquals(MISSING, UnitFormat.temp(null, TempUnit.C))
        assertEquals(MISSING, UnitFormat.wind(null, WindUnit.KMH))
        assertEquals(MISSING, UnitFormat.pressure(null, PressureUnit.HPA))
        assertEquals(MISSING, UnitFormat.precip(null, PrecipUnit.MM))
        assertEquals(MISSING, UnitFormat.percent(null))
        assertEquals(MISSING, UnitFormat.visibility(null, true))
        assertEquals(MISSING, UnitFormat.compass(null))
        assertEquals("0.0 mm", UnitFormat.precip(0.0, PrecipUnit.MM)) // a real zero stays a zero
    }
    @Test fun windPressurePrecipConversions() {
        assertEquals("100 km/h", UnitFormat.wind(100.0, WindUnit.KMH))
        assertEquals("62 mph", UnitFormat.wind(100.0, WindUnit.MPH))
        assertEquals("1013 hPa", UnitFormat.pressure(1013.0, PressureUnit.HPA))
        assertEquals("29.91 inHg", UnitFormat.pressure(1013.0, PressureUnit.INHG))
        assertEquals("25.4 mm", UnitFormat.precip(25.4, PrecipUnit.MM))
        assertEquals("1.00 in", UnitFormat.precip(25.4, PrecipUnit.IN))
        assertEquals("10.0 km", UnitFormat.visibility(10000.0, true))
        assertEquals("6.2 mi", UnitFormat.visibility(10000.0, false))
    }
    @Test fun temperatureDeltaHasNoOffset() {
        assertEquals(9, UnitFormat.tempDelta(5.0, TempUnit.F))
        assertEquals(5, UnitFormat.tempDelta(5.0, TempUnit.C))
    }
    @Test fun compassPoints() {
        assertEquals("N", UnitFormat.compass(0))
        assertEquals("N", UnitFormat.compass(359))
        assertEquals("E", UnitFormat.compass(90))
        assertEquals("S", UnitFormat.compass(180))
        assertEquals("W", UnitFormat.compass(270))
        assertEquals("NE", UnitFormat.compass(45))
    }
    @Test fun timeFormats() {
        assertEquals("18:05", UnitFormat.time(LocalTime.of(18, 5), true, Locale.ENGLISH))
        assertEquals("6:05 PM", UnitFormat.time(LocalTime.of(18, 5), false, Locale.US))
        assertEquals("07:30", UnitFormat.minutesToClock(7 * 60 + 30, true, Locale.ENGLISH))
    }
    @Test fun thresholdRoundTrip() {
        assertEquals(100, UnitFormat.thresholdTemp(38, TempUnit.F))
        assertEquals(38, UnitFormat.thresholdToCelsius(100, TempUnit.F))
        assertEquals(38, UnitFormat.thresholdTemp(38, TempUnit.C))
    }
    @Test fun aqiLabelsKeepScalesSeparate() {
        assertEquals("Good", UnitFormat.aqiLabelUs(40))
        assertEquals("Unhealthy for sensitive groups", UnitFormat.aqiLabelUs(120))
        assertEquals("Fair", UnitFormat.aqiLabelEu(30))
        assertEquals("Extremely poor", UnitFormat.aqiLabelEu(150))
    }
}
