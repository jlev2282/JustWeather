package com.justweather.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherEntity(
    // Cache key is the user-selected location query.
    @PrimaryKey val cityQuery: String,
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val tempCelsius: Double,
    val humidityPercent: Int,
    val windSpeedMetersPerSecond: Double,
    val updatedAtEpochMs: Long,
)
