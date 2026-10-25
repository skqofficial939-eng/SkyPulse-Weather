package com.skypulse.weather

import com.skypulse.weather.domain.AlertCategory
import com.skypulse.weather.domain.MonitorScope
import com.skypulse.weather.domain.Place
import com.skypulse.weather.domain.SettingsState
import com.skypulse.weather.notify.AlertDispatcher
import com.skypulse.weather.notify.MonitorPolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AlertDispatcherTest {
    private val place = Place(1L, "Okara", "Punjab", "Pakistan", "PK", 30.81, 73.45)
    private val start = LocalDateTime.of(2026, 10, 24, 17, 0)
    private val rainy = snapshot(hours = (0 until 30).map { hour(start.plusHours(it.toLong()), prob = if (it == 2) 90 else 0) })
    private val on = SettingsState(notificationsEnabled = true, enabledCategories = setOf(AlertCategory.RAIN_SOON), quietEnabled = false)
    private val noon = LocalDateTime.of(2026, 10, 24, 12, 0)

    @Test fun duplicateAlertsAreNotSentTwice() = runBlocking {
        val store = FakeStore(); val d = AlertDispatcher(store); val sent = mutableListOf<String>()
        val first = d.dispatch(place, rainy, on, NOW_MILLIS, noon) { sent += it.key; true }
        val second = d.dispatch(place, rainy, on, NOW_MILLIS, noon.plusMinutes(30)) { sent += it.key; true }
        assertEquals(1, first); assertEquals(0, second); assertEquals(1, sent.size)
    }
    @Test fun masterSwitchOffSendsNothing() = runBlocking {
        val d = AlertDispatcher(FakeStore())
        assertEquals(0, d.dispatch(place, rainy, on.copy(notificationsEnabled = false), NOW_MILLIS, noon) { true })
    }
    @Test fun quietHoursSuppressAndDoNotConsumeTheAlert() = runBlocking {
        val store = FakeStore(); val d = AlertDispatcher(store)
        val quiet = on.copy(quietEnabled = true, quietStartMin = 22 * 60, quietEndMin = 7 * 60)
        assertEquals(0, d.dispatch(place, rainy, quiet, NOW_MILLIS, LocalDateTime.of(2026, 10, 24, 23, 0)) { true })
        assertTrue(store.claimed.isEmpty())
        // after quiet hours end the alert is still deliverable
        assertEquals(1, d.dispatch(place, rainy, quiet, NOW_MILLIS, LocalDateTime.of(2026, 10, 25, 8, 0)) { true })
    }
    @Test fun categoryToggleControlsWhatIsSent() = runBlocking {
        val d = AlertDispatcher(FakeStore())
        assertEquals(0, d.dispatch(place, rainy, on.copy(enabledCategories = setOf(AlertCategory.HEAT)), NOW_MILLIS, noon) { true })
    }
    @Test fun morningSummarySentOncePerDayInsideItsWindow() = runBlocking {
        val store = FakeStore(); val d = AlertDispatcher(store)
        val s = on.copy(enabledCategories = setOf(AlertCategory.MORNING_SUMMARY), morningMin = 7 * 60 + 30)
        assertEquals(0, d.dispatch(place, snapshot(), s, NOW_MILLIS, LocalDateTime.of(2026, 10, 24, 7, 0)) { true })
        assertEquals(1, d.dispatch(place, snapshot(), s, NOW_MILLIS, LocalDateTime.of(2026, 10, 24, 8, 0)) { true })
        assertEquals(0, d.dispatch(place, snapshot(), s, NOW_MILLIS, LocalDateTime.of(2026, 10, 24, 9, 0)) { true })
        assertEquals(0, d.dispatch(place, snapshot(), s, NOW_MILLIS, LocalDateTime.of(2026, 10, 24, 11, 0)) { true }) // window over
    }
    @Test fun failedSendIsNotCountedAsSent() = runBlocking {
        assertEquals(0, AlertDispatcher(FakeStore()).dispatch(place, rainy, on, NOW_MILLIS, noon) { false })
    }
    @Test fun monitorPolicy() {
        val a = Place(1, "A", null, null, null, 0.0, 0.0, isFavorite = true)
        val b = Place(2, "B", null, null, null, 0.0, 0.0)
        val c = Place(3, "C", null, null, null, 0.0, 0.0, isFavorite = true)
        val all = listOf(a, b, c)
        assertEquals(listOf(b), MonitorPolicy.select(MonitorScope.SELECTED_ONLY, 2, all))
        assertEquals(listOf(a, c), MonitorPolicy.select(MonitorScope.FAVORITES, 2, all))
        assertEquals(listOf(b), MonitorPolicy.select(MonitorScope.FAVORITES, 2, listOf(b)))
        assertEquals(all, MonitorPolicy.select(MonitorScope.ALL_SAVED, 2, all))
    }
}
