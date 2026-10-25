package com.skypulse.weather.notify

import com.skypulse.weather.domain.NotifiedStore
import com.skypulse.weather.domain.AlertCandidate
import com.skypulse.weather.domain.AlertCategory
import com.skypulse.weather.domain.AlertRules
import com.skypulse.weather.domain.MonitorScope
import com.skypulse.weather.domain.Place
import com.skypulse.weather.domain.QuietHours
import com.skypulse.weather.domain.SettingsState
import com.skypulse.weather.domain.SummaryTiming
import com.skypulse.weather.domain.WeatherSnapshot
import java.time.LocalDateTime

object MonitorPolicy {
    fun select(scope: MonitorScope, selectedId: Long, all: List<Place>): List<Place> = when (scope) {
        MonitorScope.SELECTED_ONLY -> all.filter { it.id == selectedId }
        MonitorScope.FAVORITES -> all.filter { it.isFavorite }.ifEmpty { all.filter { it.id == selectedId } }
        MonitorScope.ALL_SAVED -> all
    }
}

/**
 * Decides which notifications to send for one place. Pure apart from the [NotifiedStore], so it is
 * unit-testable with a fake store. [deviceNow] is the user's own clock (quiet hours and summary
 * times are personal, not tied to the forecast location's time zone).
 */
class AlertDispatcher(private val store: NotifiedStore) {

    suspend fun dispatch(
        place: Place,
        snap: WeatherSnapshot,
        s: SettingsState,
        nowMillis: Long,
        deviceNow: LocalDateTime,
        send: (AlertCandidate) -> Boolean,
    ): Int {
        if (!s.notificationsEnabled) return 0
        val nowMin = QuietHours.minutesOfDay(deviceNow.toLocalTime())
        if (QuietHours.isQuiet(s.quietEnabled, nowMin, s.quietStartMin, s.quietEndMin)) return 0

        val candidates = mutableListOf<AlertCandidate>()
        candidates += AlertRules.evaluate(place.id, place.name, snap, s, nowMillis)

        if (AlertCategory.MORNING_SUMMARY in s.enabledCategories && SummaryTiming.isDue(nowMin, s.morningMin)) {
            AlertRules.summary(AlertCategory.MORNING_SUMMARY, place.id, place.name, snap, s, deviceNow.toLocalDate(), nowMillis)
                ?.let { candidates += it }
        }
        if (AlertCategory.EVENING_SUMMARY in s.enabledCategories && SummaryTiming.isDue(nowMin, s.eveningMin)) {
            AlertRules.summary(AlertCategory.EVENING_SUMMARY, place.id, place.name, snap, s, deviceNow.toLocalDate(), nowMillis)
                ?.let { candidates += it }
        }

        var sent = 0
        for (c in candidates) {
            if (store.claim(c.key) && send(c)) sent++
        }
        return sent
    }
}
