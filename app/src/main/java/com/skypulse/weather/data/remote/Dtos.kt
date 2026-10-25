package com.skypulse.weather.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponseDto(val results: List<GeoResultDto>? = null)

@Serializable
data class GeoResultDto(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val admin1: String? = null,
    val timezone: String? = null,
)

@Serializable
data class ForecastDto(
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val timezone: String? = null,
    val current: CurrentDto? = null,
    val hourly: HourlyDto? = null,
    val daily: DailyDto? = null,
)

@Serializable
data class CurrentDto(
    val time: String? = null,
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("apparent_temperature") val apparent: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Int? = null,
    @SerialName("is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("pressure_msl") val pressure: Double? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    @SerialName("wind_direction_10m") val windDirection: Int? = null,
    @SerialName("wind_gusts_10m") val windGusts: Double? = null,
)

@Serializable
data class HourlyDto(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("apparent_temperature") val apparent: List<Double?> = emptyList(),
    @SerialName("precipitation_probability") val precipProb: List<Int?> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed: List<Double?> = emptyList(),
    @SerialName("relative_humidity_2m") val humidity: List<Int?> = emptyList(),
    @SerialName("uv_index") val uv: List<Double?> = emptyList(),
    val visibility: List<Double?> = emptyList(),
    @SerialName("dew_point_2m") val dewPoint: List<Double?> = emptyList(),
    @SerialName("is_day") val isDay: List<Int?> = emptyList(),
)

@Serializable
data class DailyDto(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val tempMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val tempMin: List<Double?> = emptyList(),
    val sunrise: List<String?> = emptyList(),
    val sunset: List<String?> = emptyList(),
    @SerialName("uv_index_max") val uvMax: List<Double?> = emptyList(),
    @SerialName("precipitation_sum") val precipSum: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipProbMax: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m_max") val windMax: List<Double?> = emptyList(),
)

@Serializable
data class AirQualityDto(val current: AirCurrentDto? = null)

@Serializable
data class AirCurrentDto(
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("european_aqi") val euAqi: Int? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    val pm10: Double? = null,
)
