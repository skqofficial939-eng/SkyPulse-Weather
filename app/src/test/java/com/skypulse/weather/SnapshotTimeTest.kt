package com.skypulse.weather

import com.skypulse.weather.domain.Insights
import com.skypulse.weather.domain.SettingsState
import com.skypulse.weather.domain.WeatherCodes
import com.skypulse.weather.domain.WeatherKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SnapshotTimeTest {
    @Test fun localTimeUsesThePlaceOffsetNotTheDevice() {
        assertEquals(LocalDateTime.of(2026, 10, 24, 18, 0), snapshot(offset = 5 * 3600).localNow(NOW_MILLIS))      // Karachi
        assertEquals(LocalDateTime.of(2026, 10, 24, 22, 30), snapshot(offset = 9 * 3600 + 1800).localNow(NOW_MILLIS)) // Adelaide-ish
        assertEquals(LocalDateTime.of(2026, 10, 24, 9, 0), snapshot(offset = -4 * 3600).localNow(NOW_MILLIS))     // New York (EDT)
    }
    @Test fun dateBoundaryRollsOver() {
        val lateUtc = LocalDateTime.of(2026, 10, 24, 23, 30).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        assertEquals(LocalDate.of(2026, 10, 25), snapshot(offset = 5 * 3600).localNow(lateUtc).toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 24), snapshot(offset = -5 * 3600).localNow(lateUtc).toLocalDate())
    }
    @Test fun dstOffsetChangeIsReflectedByTheOffsetInTheResponse() {
        // Offsets come from the API per response, so a place that moved from +1 to +0 on a DST end shows the new local hour.
        val t = LocalDateTime.of(2026, 10, 25, 1, 30).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        assertEquals(2, snapshot(offset = 3600).localNow(t).hour)
        assertEquals(1, snapshot(offset = 0).localNow(t).hour)
    }
    @Test fun upcomingHoursStartAtTheCurrentLocalHourAndStayOrdered() {
        val ups = snapshot().upcomingHours(NOW_MILLIS, 5)
        assertEquals(LocalDateTime.of(2026, 10, 24, 18, 0), ups.first().time)
        assertEquals(5, ups.size)
        assertEquals(ups.sortedBy { it.time }, ups)
    }
    @Test fun weatherCodeMapping() {
        assertEquals(WeatherKind.CLEAR, WeatherCodes.kind(0))
        assertEquals(WeatherKind.RAIN, WeatherCodes.kind(63))
        assertEquals(WeatherKind.STORM, WeatherCodes.kind(95))
        assertEquals(WeatherKind.UNKNOWN, WeatherCodes.kind(null))
        assertEquals("Conditions unavailable", WeatherCodes.describe(null))
    }
    @Test fun insightsOnlyUseRealValues() {
        val calm = Insights.generate(snapshot(), SettingsState(use24h = true), NOW_MILLIS)
        assertTrue(calm.any { it.contains("No significant precipitation") })
        val noProb = snapshot(hours = (0 until 30).map { hour(LocalDateTime.of(2026, 10, 24, 17, 0).plusHours(it.toLong()), prob = null) })
        assertTrue(Insights.generate(noProb, SettingsState(), NOW_MILLIS).none { it.contains("precipitation", ignoreCase = true) })
    }
}
