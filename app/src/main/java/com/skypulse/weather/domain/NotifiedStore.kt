package com.skypulse.weather.domain

/** Persistent de-duplication store for notifications (Room-backed in production, a fake in tests). */
interface NotifiedStore {
    /** Returns true exactly once per key (the caller may notify); false if it was already claimed. */
    suspend fun claim(key: String): Boolean
    suspend fun pruneOlderThan(millis: Long)
}
