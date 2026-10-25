package com.skypulse.weather.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Open-Meteo endpoints. Absolute URLs are used so one Retrofit instance can talk to the
 * forecast, geocoding and air-quality hosts. No API key is required for these endpoints
 * (free tier is for non-commercial use; see README).
 */
interface OpenMeteoApi {
    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("hourly") hourly: String = HOURLY_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("forecast_days") days: Int = 10,
        @Query("timezone") timezone: String = "auto",
    ): ForecastDto

    @GET("https://air-quality-api.open-meteo.com/v1/air-quality")
    suspend fun airQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,european_aqi,pm2_5,pm10",
        @Query("timezone") timezone: String = "auto",
    ): AirQualityDto

    @GET("https://geocoding-api.open-meteo.com/v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 12,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponseDto

    companion object {
        const val CURRENT_FIELDS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code," +
                "pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m"
        const val HOURLY_FIELDS =
            "temperature_2m,apparent_temperature,precipitation_probability,precipitation,weather_code," +
                "wind_speed_10m,relative_humidity_2m,uv_index,visibility,dew_point_2m,is_day"
        const val DAILY_FIELDS =
            "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max," +
                "precipitation_sum,precipitation_probability_max,wind_speed_10m_max"
    }
}

val AppJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

fun buildOpenMeteoApi(): OpenMeteoApi {
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build() // deliberately no logging interceptor: URLs contain precise coordinates
    return Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenMeteoApi::class.java)
}
