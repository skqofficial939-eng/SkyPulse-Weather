package com.skypulse.weather.domain

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

const val MISSING = "—" // em dash: shown when a value was not provided (never "0")

object UnitFormat {
    fun cToF(c: Double) = c * 9.0 / 5.0 + 32.0
    fun kmhToMph(v: Double) = v * 0.621371
    fun hpaToInHg(v: Double) = v * 0.02953
    fun mmToIn(v: Double) = v / 25.4

    private fun Double.roundedNoNegZero(): Int {
        val r = roundToInt()
        return if (r == 0) 0 else r
    }

    fun temp(c: Double?, unit: TempUnit, withSymbol: Boolean = true): String {
        if (c == null) return MISSING
        val v = if (unit == TempUnit.C) c else cToF(c)
        return v.roundedNoNegZero().toString() + if (withSymbol) "°" else ""
    }

    /** Temperature with the unit letter, e.g. "30°C". */
    fun tempWithUnit(c: Double?, unit: TempUnit): String =
        if (c == null) MISSING else temp(c, unit) + unit.name

    /** A temperature *difference* converted for display (no +32 offset). */
    fun tempDelta(deltaC: Double, unit: TempUnit): Int =
        (if (unit == TempUnit.C) deltaC else deltaC * 9.0 / 5.0).roundedNoNegZero()

    fun wind(kmh: Double?, unit: WindUnit): String {
        if (kmh == null) return MISSING
        return when (unit) {
            WindUnit.KMH -> "${kmh.roundedNoNegZero()} km/h"
            WindUnit.MPH -> "${kmhToMph(kmh).roundedNoNegZero()} mph"
        }
    }

    fun pressure(hpa: Double?, unit: PressureUnit): String {
        if (hpa == null) return MISSING
        return when (unit) {
            PressureUnit.HPA -> "${hpa.roundToInt()} hPa"
            PressureUnit.INHG -> String.format(Locale.ENGLISH, "%.2f inHg", hpaToInHg(hpa))
        }
    }

    fun precip(mm: Double?, unit: PrecipUnit): String {
        if (mm == null) return MISSING
        return when (unit) {
            PrecipUnit.MM -> String.format(Locale.ENGLISH, "%.1f mm", mm)
            PrecipUnit.IN -> String.format(Locale.ENGLISH, "%.2f in", mmToIn(mm))
        }
    }

    fun percent(v: Int?): String = if (v == null) MISSING else "$v%"

    fun visibility(meters: Double?, metric: Boolean): String {
        if (meters == null) return MISSING
        val km = meters / 1000.0
        return if (metric) String.format(Locale.ENGLISH, "%.1f km", km)
        else String.format(Locale.ENGLISH, "%.1f mi", km * 0.621371)
    }

    private val COMPASS = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")

    fun compass(deg: Int?): String {
        if (deg == null) return MISSING
        val idx = (((deg % 360) + 360) % 360 / 22.5 + 0.5).toInt() % 16
        return COMPASS[idx]
    }

    fun time(t: LocalTime, use24h: Boolean, locale: Locale = Locale.getDefault()): String =
        t.format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm a", locale))

    fun time(t: LocalDateTime?, use24h: Boolean, locale: Locale = Locale.getDefault()): String =
        if (t == null) MISSING else time(t.toLocalTime(), use24h, locale)

    fun hourLabel(t: LocalDateTime, use24h: Boolean, locale: Locale = Locale.getDefault()): String =
        t.format(DateTimeFormatter.ofPattern(if (use24h) "HH" else "ha", locale)).lowercase(locale)

    fun minutesToClock(minutes: Int, use24h: Boolean, locale: Locale = Locale.getDefault()): String =
        time(LocalTime.of((minutes / 60) % 24, minutes % 60), use24h, locale)

    /** Heat/cold thresholds are stored in Celsius; the UI shows them in the user's unit. */
    fun thresholdTemp(c: Int, unit: TempUnit): Int = if (unit == TempUnit.C) c else cToF(c.toDouble()).roundToInt()
    fun thresholdToCelsius(shown: Int, unit: TempUnit): Int =
        if (unit == TempUnit.C) shown else ((shown - 32) * 5.0 / 9.0).roundToInt()

    fun aqiLabelUs(aqi: Int?): String = when {
        aqi == null -> MISSING
        aqi <= 50 -> "Good"
        aqi <= 100 -> "Moderate"
        aqi <= 150 -> "Unhealthy for sensitive groups"
        aqi <= 200 -> "Unhealthy"
        aqi <= 300 -> "Very unhealthy"
        else -> "Hazardous"
    }

    fun aqiLabelEu(aqi: Int?): String = when {
        aqi == null -> MISSING
        aqi <= 20 -> "Good"
        aqi <= 40 -> "Fair"
        aqi <= 60 -> "Moderate"
        aqi <= 80 -> "Poor"
        aqi <= 100 -> "Very poor"
        else -> "Extremely poor"
    }
}
