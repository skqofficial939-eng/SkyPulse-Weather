package com.skypulse.weather.domain

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * A notification the app *could* send. These are app-generated advisories derived from forecast
 * numbers. They are NOT official government warnings and are never labelled as such.
 */
data class AlertCandidate(
    val category: AlertCategory,
    val key: String,
    val title: String,
    val message: String,
)

object AlertRules {

    fun evaluate(
        placeId: Long,
        placeName: String,
        snap: WeatherSnapshot,
        s: SettingsState,
        nowMillis: Long,
        categories: Set<AlertCategory> = s.enabledCategories,
    ): List<AlertCandidate> {
        val out = mutableListOf<AlertCandidate>()
        val nowLocal = snap.localNow(nowMillis)
        val today = nowLocal.toLocalDate()
        val next24 = snap.upcomingHours(nowMillis, 24)
        val todayDay = snap.daily.firstOrNull { it.date == today }
        val tomorrowDay = snap.daily.firstOrNull { it.date == today.plusDays(1) }
        val use24h = s.use24h

        if (AlertCategory.RAIN_SOON in categories) {
            val window = snap.upcomingHours(nowMillis, s.rainWindowHours + 1)
            val hit = window.firstOrNull { (it.precipProb ?: -1) >= s.rainProbPct }
            if (hit != null) {
                val peak = window.maxOf { it.precipProb ?: -1 }
                out += AlertCandidate(
                    AlertCategory.RAIN_SOON,
                    "rain:$placeId:${hit.time.toLocalDate()}:${hit.time.hour / 6}",
                    "Rain possible in $placeName",
                    "Rain may begin soon in $placeName: up to a $peak% chance within the next ${s.rainWindowHours} hours, " +
                        "around ${UnitFormat.time(hit.time, use24h)}. Check the latest forecast before heading out.",
                )
            }
        }

        if (AlertCategory.HIGH_PRECIP in categories) {
            val p = todayDay?.precipProbMax
            if (p != null && p >= s.highPrecipPct) {
                out += AlertCandidate(
                    AlertCategory.HIGH_PRECIP, "precip:$placeId:$today",
                    "High chance of precipitation today",
                    "The forecast for $placeName shows up to a $p% chance of precipitation today.",
                )
            }
        }

        if (AlertCategory.THUNDERSTORM in categories) {
            val hit = next24.take(12).firstOrNull { WeatherCodes.isThunder(it.code) }
            if (hit != null) {
                out += AlertCandidate(
                    AlertCategory.THUNDERSTORM, "thunder:$placeId:${hit.time.toLocalDate()}:${hit.time.hour / 6}",
                    "Thunderstorms in the forecast",
                    "The forecast for $placeName includes thunderstorms around ${UnitFormat.time(hit.time, use24h)}. " +
                        "Conditions can change quickly, so stay alert.",
                )
            }
        }

        if (AlertCategory.HEAT in categories) {
            val peak = next24.mapNotNull { it.tempC }.maxOrNull()
            if (peak != null && peak >= s.heatC) {
                out += AlertCandidate(
                    AlertCategory.HEAT, "heat:$placeId:$today", "High temperatures expected",
                    "Temperatures in $placeName may reach ${UnitFormat.tempWithUnit(peak, s.tempUnit)} in the next 24 hours. " +
                        "Stay hydrated and limit time in direct sun.",
                )
            }
        }

        if (AlertCategory.COLD in categories) {
            val low = next24.mapNotNull { it.tempC }.minOrNull()
            if (low != null && low <= s.coldC) {
                out += AlertCandidate(
                    AlertCategory.COLD, "cold:$placeId:$today", "Low temperatures expected",
                    "Temperatures in $placeName may drop to ${UnitFormat.tempWithUnit(low, s.tempUnit)} in the next 24 hours. " +
                        "Dress warmly.",
                )
            }
        }

        if (AlertCategory.WIND in categories) {
            val peak = next24.mapNotNull { it.windKmh }.maxOrNull()
            if (peak != null && peak >= s.windKmh) {
                out += AlertCandidate(
                    AlertCategory.WIND, "wind:$placeId:$today", "Strong winds forecast",
                    "Winds around ${UnitFormat.wind(peak, WindUnit.KMH)} are forecast for $placeName in the next 24 hours.",
                )
            }
        }

        if (AlertCategory.UV in categories) {
            val uv = todayDay?.uvMax
            if (uv != null && uv >= s.uvIndex) {
                out += AlertCandidate(
                    AlertCategory.UV, "uv:$placeId:$today", "High UV today",
                    "The UV index in $placeName may reach ${uv.roundToInt()} today. Use sun protection.",
                )
            }
        }

        if (AlertCategory.SIGNIFICANT_CHANGE in categories) {
            val t = todayDay?.maxC
            val n = tomorrowDay?.maxC
            if (t != null && n != null && kotlin.math.abs(n - t) >= SIGNIFICANT_CHANGE_C) {
                val warmer = n > t
                val delta = UnitFormat.tempDelta(kotlin.math.abs(n - t), s.tempUnit)
                out += AlertCandidate(
                    AlertCategory.SIGNIFICANT_CHANGE, "change:$placeId:${today.plusDays(1)}",
                    "Big temperature change tomorrow",
                    "Tomorrow's high in $placeName is forecast to be about $delta° ${if (warmer) "warmer" else "cooler"} than today.",
                )
            }
        }
        return out
    }

    const val SIGNIFICANT_CHANGE_C = 8.0

    /** Morning summary describes today; evening summary describes tomorrow. Returns null if data is missing. */
    fun summary(
        category: AlertCategory,
        placeId: Long,
        placeName: String,
        snap: WeatherSnapshot,
        s: SettingsState,
        deviceDate: LocalDate,
        nowMillis: Long,
    ): AlertCandidate? {
        val placeToday = snap.localNow(nowMillis).toLocalDate()
        val target = when (category) {
            AlertCategory.MORNING_SUMMARY -> placeToday
            AlertCategory.EVENING_SUMMARY -> placeToday.plusDays(1)
            else -> return null
        }
        val day = snap.daily.firstOrNull { it.date == target } ?: return null
        val label = if (category == AlertCategory.MORNING_SUMMARY) "Today" else "Tomorrow"
        val range = "${UnitFormat.temp(day.minC, s.tempUnit)}–${UnitFormat.temp(day.maxC, s.tempUnit)}${s.tempUnit.name}"
        val rain = day.precipProbMax?.let { " Chance of precipitation up to $it%." }.orEmpty()
        val kind = if (category == AlertCategory.MORNING_SUMMARY) "morning" else "evening"
        return AlertCandidate(
            category, "summary:$kind:$placeId:$deviceDate",
            if (category == AlertCategory.MORNING_SUMMARY) "Good morning — $placeName" else "Tomorrow in $placeName",
            "$label in $placeName: ${WeatherCodes.describe(day.code).lowercase()}, $range.$rain",
        )
    }
}

object SummaryTiming {
    /** Background jobs are inexact, so a summary is "due" for a window after its target time. */
    const val WINDOW_MINUTES = 180
    fun isDue(nowMin: Int, targetMin: Int, window: Int = WINDOW_MINUTES): Boolean =
        nowMin >= targetMin && nowMin < targetMin + window
}

object Insights {
    fun generate(snap: WeatherSnapshot, s: SettingsState, nowMillis: Long): List<String> {
        val out = mutableListOf<String>()
        val next24 = snap.upcomingHours(nowMillis, 24)
        val probs = next24.mapNotNull { it.precipProb }
        if (probs.isNotEmpty()) {
            val likely = next24.firstOrNull { (it.precipProb ?: -1) >= 50 }
            if (likely != null) {
                out += "Precipitation looks likely around ${UnitFormat.time(likely.time, s.use24h)} (${likely.precipProb}% chance)."
            } else if (probs.max() < 20) {
                out += "No significant precipitation is forecast for the next 24 hours."
            }
        }
        val today = snap.localNow(nowMillis).toLocalDate()
        val t = snap.daily.firstOrNull { it.date == today }?.maxC
        val n = snap.daily.firstOrNull { it.date == today.plusDays(1) }?.maxC
        if (t != null && n != null && kotlin.math.abs(n - t) >= 3.0) {
            val d = UnitFormat.tempDelta(kotlin.math.abs(n - t), s.tempUnit)
            out += "Tomorrow's high is about $d° ${if (n > t) "warmer" else "cooler"} than today's."
        }
        val uv = snap.daily.firstOrNull { it.date == today }?.uvMax
        if (uv != null && uv >= 6) out += "UV index peaks near ${uv.roundToInt()} today. Sun protection is advised."
        val feels = snap.current.feelsC
        val temp = snap.current.tempC
        if (feels != null && temp != null && kotlin.math.abs(feels - temp) >= 4.0) {
            out += "It feels noticeably ${if (feels > temp) "warmer" else "cooler"} than the air temperature."
        }
        val gust = snap.current.gustKmh
        if (gust != null && gust >= 40) out += "Gusts up to ${UnitFormat.wind(gust, s.windUnit)} right now."
        return out
    }
}
