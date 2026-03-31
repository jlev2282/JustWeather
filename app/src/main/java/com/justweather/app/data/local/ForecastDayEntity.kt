package com.justweather.app.data.local

import androidx.room.Entity

@Entity(
    tableName = "forecast_cache",
    primaryKeys = ["cityQuery", "dayIndex"],
)
data class ForecastDayEntity(
    val cityQuery: String,
    val dayIndex: Int,
    val dayEpochSeconds: Long,
    val minTempCelsius: Double,
    val maxTempCelsius: Double,
    val weatherCode: Int?,
)

