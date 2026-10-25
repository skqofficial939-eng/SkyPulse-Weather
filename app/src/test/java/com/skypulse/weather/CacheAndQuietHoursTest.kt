package com.skypulse.weather

import com.skypulse.weather.domain.CachePolicy
import com.skypulse.weather.domain.QuietHours
import com.skypulse.weather.domain.SummaryTiming
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheAndQuietHoursTest {
    @Test fun cacheFreshnessBoundary() {
        val t0 = 1_000_000L
        assertTrue(CachePolicy.isFresh(t0, t0 + 14 * 60_000L))
        assertFalse(CachePolicy.isFresh(t0, t0 + 15 * 60_000L))
        assertFalse(CachePolicy.isFresh(t0, t0 + 3 * 3600_000L))
    }
    @Test fun clockMovedBackwardsIsNotFresh() {
        assertFalse(CachePolicy.isFresh(2_000_000L, 1_000_000L))
    }
    @Test fun quietHoursWrapAcrossMidnight() {
        val start = 22 * 60; val end = 7 * 60
        assertTrue(QuietHours.isQuiet(true, 23 * 60, start, end))
        assertTrue(QuietHours.isQuiet(true, 0, start, end))
        assertTrue(QuietHours.isQuiet(true, 6 * 60 + 59, start, end))
        assertFalse(QuietHours.isQuiet(true, 7 * 60, start, end))
        assertFalse(QuietHours.isQuiet(true, 12 * 60, start, end))
        assertTrue(QuietHours.isQuiet(true, 22 * 60, start, end))
    }
    @Test fun quietHoursSameDayWindowAndDisabled() {
        assertTrue(QuietHours.isQuiet(true, 13 * 60, 12 * 60, 14 * 60))
        assertFalse(QuietHours.isQuiet(true, 15 * 60, 12 * 60, 14 * 60))
        assertFalse(QuietHours.isQuiet(false, 23 * 60, 22 * 60, 7 * 60))
        assertFalse(QuietHours.isQuiet(true, 3 * 60, 8 * 60, 8 * 60)) // equal bounds = never quiet
    }
    @Test fun summaryWindow() {
        val target = 7 * 60 + 30
        assertFalse(SummaryTiming.isDue(7 * 60, target))
        assertTrue(SummaryTiming.isDue(target, target))
        assertTrue(SummaryTiming.isDue(target + 179, target))
        assertFalse(SummaryTiming.isDue(target + 180, target))
    }
}
