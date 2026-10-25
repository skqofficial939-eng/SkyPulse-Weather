package com.skypulse.weather.notify

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.skypulse.weather.SkyPulseApp
import com.skypulse.weather.domain.SettingsState
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Periodic, deferrable weather check. Android decides the exact run time (battery, Doze, standby
 * buckets), so alerts are best-effort and can arrive late. Nothing here tracks location.
 */
class WeatherCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as SkyPulseApp).container
        val s = container.settings.current()
        if (!s.notificationsEnabled || s.enabledCategories.isEmpty()) return Result.success()
        if (!Notifier.canNotify(applicationContext)) return Result.success() // permission denied: nothing to do

        val places = MonitorPolicy.select(s.monitorScope, s.selectedPlaceId, container.places.all())
        if (places.isEmpty()) return Result.success()

        container.notified.pruneOlderThan(3L * 24 * 60 * 60 * 1000)
        val dispatcher = AlertDispatcher(container.notified)
        var failures = 0
        for (place in places) {
            val snap = container.weather.refresh(place).getOrNull()
            if (snap == null) { failures++; continue }
            dispatcher.dispatch(place, snap, s, System.currentTimeMillis(), LocalDateTime.now()) { c ->
                Notifier.show(applicationContext, c, place.id)
            }
        }
        // Retry (with exponential backoff) only if every place failed, e.g. no connectivity.
        return if (failures == places.size && runAttemptCount < 3) Result.retry() else Result.success()
    }
}

object Scheduler {
    private const val WORK_NAME = "skypulse_weather_check"

    fun apply(context: Context, s: SettingsState) {
        val wm = WorkManager.getInstance(context)
        if (!s.notificationsEnabled || s.enabledCategories.isEmpty()) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<WeatherCheckWorker>(s.refreshHours.coerceAtLeast(1).toLong(), TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
