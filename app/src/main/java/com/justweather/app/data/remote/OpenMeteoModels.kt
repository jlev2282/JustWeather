package com.justweather.app.data.remote

data class OpenMeteoCurrentResponse(
    val current: OpenMeteoCurrentBlock,
)

data class OpenMeteoCurrentBlock(
    val temperature_2m: Double,
    val precipitation: Double,
    val weather_code: Int,
)

