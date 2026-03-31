package com.justweather.app.data.remote

data class OpenWeatherResponse(
    val name: String,
    val main: MainBlock,
    val wind: WindBlock,
    val coord: CoordBlock,
)

data class MainBlock(
    val temp: Double,
    val humidity: Int,
)

data class WindBlock(
    val speed: Double,
)

data class CoordBlock(
    val lat: Double,
    val lon: Double,
)

data class ForecastResponse(
    val daily: List<ForecastDailyBlock>,
)

data class ForecastDailyBlock(
    val dt: Long,
    val temp: ForecastTempBlock,
)

data class ForecastTempBlock(
    val min: Double,
    val max: Double,
)

data class GeoLocationResponse(
    val name: String,
    val state: String?,
    val country: String,
    val lat: Double,
    val lon: Double,
)
