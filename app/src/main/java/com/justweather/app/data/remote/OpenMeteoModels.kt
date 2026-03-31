package com.justweather.app.data.remote

data class OpenMeteoCurrentResponse(
    val current: OpenMeteoCurrentBlock,
)

data class OpenMeteoDailyForecastResponse(
    val daily: OpenMeteoDailyBlock,
)

data class OpenMeteoDailyBlock(
    val time: List<String>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val weather_code: List<Int>? = null,
)

data class OpenMeteoCurrentBlock(
    val temperature_2m: Double,
    val apparent_temperature: Double,
    val precipitation: Double,
    val weather_code: Int,
)

