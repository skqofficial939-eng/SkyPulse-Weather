package com.skypulse.weather

import com.skypulse.weather.domain.AlertCategory
import com.skypulse.weather.domain.AlertRules
import com.skypulse.weather.domain.SettingsState
import com.skypulse.weather.domain.TempUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AlertRulesTest {
    private val all = AlertCategory.entries.toSet()
    private val base = SettingsState(use24h = true)
    private val start = LocalDateTime.of(2026, 10, 24, 17, 0)

    private fun eval(snap: com.skypulse.weather.domain.WeatherSnapshot, s: SettingsState = base, cats: Set<AlertCategory> = all) =
        AlertRules.evaluate(1L, "Okara", snap, s, NOW_MILLIS, cats)

    private fun hoursWithProb(vararg probByIndex: Pair<Int, Int>) = (0 until 30).map { i ->
        hour(start.plusHours(i.toLong()), prob = probByIndex.firstOrNull { it.first == i }?.second ?: 0)
    }

    @Test fun rainAlertFiresAtThresholdWithinWindow() {
        // index 2 = 19:00 local, inside the 3 h window from 18:00 (window includes 18,19,20,21)
        val snap = snapshot(hours = hoursWithProb(2 to 60))
        val r = eval(snap).single { it.category == AlertCategory.RAIN_SOON }
        assertTrue(r.message.contains("Okara"))
        assertTrue(r.message.contains("60%"))
        assertTrue(r.message.contains("19:00"))
    }
    @Test fun rainAlertWordingNeverClaimsCertainty() {
        val r = eval(snapshot(hours = hoursWithProb(2 to 95))).single { it.category == AlertCategory.RAIN_SOON }
        assertTrue(r.message.startsWith("Rain may begin soon"))
        assertTrue(r.message.contains("chance"))
        assertFalse(r.message.contains("will rain", ignoreCase = true))
        assertFalse(r.message.contains("official", ignoreCase = true))
    }
    @Test fun rainAlertDoesNotFireBelowThresholdOrOutsideWindow() {
        assertTrue(eval(snapshot(hours = hoursWithProb(2 to 59))).none { it.category == AlertCategory.RAIN_SOON })
        assertTrue(eval(snapshot(hours = hoursWithProb(10 to 100))).none { it.category == AlertCategory.RAIN_SOON })
    }
    @Test fun rainThresholdAndWindowSettingsChangeBehaviour() {
        val snap = snapshot(hours = hoursWithProb(5 to 70)) // 22:00, outside default 3 h window
        assertTrue(eval(snap).none { it.category == AlertCategory.RAIN_SOON })
        assertTrue(eval(snap, base.copy(rainWindowHours = 6)).any { it.category == AlertCategory.RAIN_SOON })
        assertTrue(eval(snap, base.copy(rainWindowHours = 6, rainProbPct = 80)).none { it.category == AlertCategory.RAIN_SOON })
    }
    @Test fun disabledCategoriesProduceNothing() {
        val snap = snapshot(hours = hoursWithProb(1 to 100))
        assertTrue(eval(snap, cats = emptySet()).isEmpty())
        assertTrue(eval(snap, cats = setOf(AlertCategory.HEAT)).none { it.category == AlertCategory.RAIN_SOON })
    }
    @Test fun keyIsStableAcrossRepeatedEvaluation() {
        val snap = snapshot(hours = hoursWithProb(2 to 80))
        assertEquals(eval(snap).map { it.key }, eval(snap).map { it.key })
    }
    @Test fun missingProbabilityIsNotTreatedAsRain() {
        val hs = (0 until 30).map { hour(start.plusHours(it.toLong()), prob = null) }
        assertTrue(eval(snapshot(hours = hs)).none { it.category == AlertCategory.RAIN_SOON })
    }
    @Test fun thunderstormUsesForecastCode() {
        val hs = (0 until 30).map { hour(start.plusHours(it.toLong()), code = if (it == 4) 95 else 1) }
        assertNotNull(eval(snapshot(hours = hs)).firstOrNull { it.category == AlertCategory.THUNDERSTORM })
    }
    @Test fun heatColdWindUvThresholds() {
        val hot = (0 until 30).map { hour(start.plusHours(it.toLong()), temp = if (it == 3) 41.0 else 30.0) }
        val heat = eval(snapshot(hours = hot)).single { it.category == AlertCategory.HEAT }
        assertTrue(heat.message.contains("41°C"))
        assertTrue(eval(snapshot(hours = hot), base.copy(tempUnit = TempUnit.F)).single { it.category == AlertCategory.HEAT }.message.contains("106°F"))
        val cold = (0 until 30).map { hour(start.plusHours(it.toLong()), temp = 3.0) }
        assertTrue(eval(snapshot(hours = cold)).any { it.category == AlertCategory.COLD })
        val windy = (0 until 30).map { hour(start.plusHours(it.toLong()), wind = 65.0) }
        assertTrue(eval(snapshot(hours = windy)).any { it.category == AlertCategory.WIND })
        val today = LocalDate.of(2026, 10, 24)
        assertTrue(eval(snapshot(days = listOf(day(today, uv = 9.0)))).any { it.category == AlertCategory.UV })
        assertTrue(eval(snapshot(days = listOf(day(today, uv = 7.0)))).none { it.category == AlertCategory.UV })
    }
    @Test fun nothingFiresOnCalmWeather() {
        assertTrue(eval(snapshot()).isEmpty())
    }
    @Test fun significantChange() {
        val today = LocalDate.of(2026, 10, 24)
        val days = listOf(day(today, max = 30.0), day(today.plusDays(1), max = 20.0))
        val c = eval(snapshot(days = days)).single { it.category == AlertCategory.SIGNIFICANT_CHANGE }
        assertTrue(c.message.contains("cooler"))
    }
    @Test fun summariesDescribeTheRightDay() {
        val today = LocalDate.of(2026, 10, 24)
        val days = listOf(day(today, max = 31.0, min = 19.0), day(today.plusDays(1), max = 25.0, min = 15.0))
        val snap = snapshot(days = days)
        val m = AlertRules.summary(AlertCategory.MORNING_SUMMARY, 1L, "Lahore", snap, base, today, NOW_MILLIS)!!
        assertTrue(m.message.contains("19°–31°C"))
        val e = AlertRules.summary(AlertCategory.EVENING_SUMMARY, 1L, "Lahore", snap, base, today, NOW_MILLIS)!!
        assertTrue(e.message.contains("15°–25°C"))
        assertTrue(e.message.startsWith("Tomorrow"))
        assertNull(AlertRules.summary(AlertCategory.RAIN_SOON, 1L, "Lahore", snap, base, today, NOW_MILLIS))
    }
    @Test fun summaryIsNullWhenDayMissing() {
        val snap = snapshot(days = listOf(day(LocalDate.of(2026, 10, 24))))
        assertNull(AlertRules.summary(AlertCategory.EVENING_SUMMARY, 1L, "X", snap, base, LocalDate.of(2026, 10, 24), NOW_MILLIS))
    }
}
