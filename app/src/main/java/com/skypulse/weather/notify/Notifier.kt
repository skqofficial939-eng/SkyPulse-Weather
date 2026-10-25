package com.skypulse.weather.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.skypulse.weather.MainActivity
import com.skypulse.weather.R
import com.skypulse.weather.domain.AlertCandidate
import com.skypulse.weather.domain.AlertCategory

object Notifier {
    private const val CH_RAIN = "sp_rain"
    private const val CH_EXTREME = "sp_extreme"
    private const val CH_SUMMARY = "sp_summary"
    private const val CH_CHANGE = "sp_change"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        fun ch(id: String, name: String, desc: String, importance: Int) =
            NotificationChannel(id, name, importance).apply { description = desc }
        nm.createNotificationChannels(
            listOf(
                ch(CH_RAIN, "Rain & precipitation", "Rain soon and high precipitation chances", NotificationManager.IMPORTANCE_DEFAULT),
                ch(CH_EXTREME, "Extreme conditions", "Thunderstorms, heat, cold, wind and UV advisories", NotificationManager.IMPORTANCE_HIGH),
                ch(CH_SUMMARY, "Daily summaries", "Morning and evening forecast summaries", NotificationManager.IMPORTANCE_LOW),
                ch(CH_CHANGE, "Weather changes", "Significant changes and test notifications", NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }

    private fun channelFor(c: AlertCategory) = when (c) {
        AlertCategory.RAIN_SOON, AlertCategory.HIGH_PRECIP -> CH_RAIN
        AlertCategory.THUNDERSTORM, AlertCategory.HEAT, AlertCategory.COLD, AlertCategory.WIND, AlertCategory.UV -> CH_EXTREME
        AlertCategory.MORNING_SUMMARY, AlertCategory.EVENING_SUMMARY -> CH_SUMMARY
        AlertCategory.SIGNIFICANT_CHANGE -> CH_CHANGE
    }

    /** POST_NOTIFICATIONS is a runtime permission only on Android 13+. */
    fun canNotify(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, candidate: AlertCandidate, placeId: Long): Boolean =
        post(context, candidate.key, channelFor(candidate.category), candidate.title, candidate.message, placeId)

    fun showTest(context: Context, placeId: Long, placeName: String?): Boolean =
        post(
            context, "test:${System.currentTimeMillis()}", CH_CHANGE, "SkyPulse test notification",
            "Notifications are working${placeName?.let { " for $it" }.orEmpty()}. Tap to open the forecast.", placeId,
        )

    private fun post(context: Context, key: String, channel: String, title: String, text: String, placeId: Long): Boolean {
        if (!canNotify(context)) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_PLACE_ID, placeId)
        }
        val pi = PendingIntent.getActivity(
            context, key.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(key.hashCode(), n)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
