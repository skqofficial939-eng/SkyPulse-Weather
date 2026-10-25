package com.skypulse.weather.domain

import java.time.LocalTime

object CachePolicy {
    const val FRESH_TTL_MILLIS = 15 * 60 * 1000L

    /** A cached forecast younger than the TTL is considered fresh and needs no network call. */
    fun isFresh(fetchedAtMillis: Long, nowMillis: Long, ttlMillis: Long = FRESH_TTL_MILLIS): Boolean =
        nowMillis >= fetchedAtMillis && nowMillis - fetchedAtMillis < ttlMillis
}

object QuietHours {
    /** Handles windows that wrap midnight (e.g. 22:00-07:00). Equal start/end means "never quiet". */
    fun isQuiet(enabled: Boolean, nowMinutes: Int, startMin: Int, endMin: Int): Boolean {
        if (!enabled || startMin == endMin) return false
        return if (startMin < endMin) nowMinutes in startMin until endMin
        else nowMinutes >= startMin || nowMinutes < endMin
    }

    fun minutesOfDay(t: LocalTime): Int = t.hour * 60 + t.minute
}
