package com.skypulse.weather.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.skypulse.weather.domain.CURRENT_LOCATION_ID
import com.skypulse.weather.domain.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * One-shot, foreground-only device location. Never runs in the background and never logs coordinates.
 * Reverse geocoding uses the Android platform Geocoder (on-device API that may call Google's location
 * backend); if it is unavailable the place is simply named "Current location".
 */
object LocationHelper {

    @SuppressLint("MissingPermission") // callers request ACCESS_COARSE_LOCATION first
    suspend fun currentCoordinates(context: Context): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (cont.isActive) cont.resume(loc?.let { it.latitude to it.longitude })
                }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }

    suspend fun toPlace(context: Context, lat: Double, lon: Double): Place = withContext(Dispatchers.IO) {
        val address = try {
            if (Geocoder.isPresent()) {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
        Place(
            id = CURRENT_LOCATION_ID,
            name = address?.locality ?: address?.subAdminArea ?: address?.adminArea ?: "Current location",
            admin1 = address?.adminArea,
            country = address?.countryName,
            countryCode = address?.countryCode,
            latitude = Math.round(lat * 1000) / 1000.0, // ~100 m is plenty for weather
            longitude = Math.round(lon * 1000) / 1000.0,
        )
    }
}
