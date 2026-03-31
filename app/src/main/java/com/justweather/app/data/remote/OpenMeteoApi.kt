package com.justweather.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun getCurrentConditions(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,apparent_temperature,precipitation,weather_code",
        @Query("temperature_unit") temperatureUnit: String = "fahrenheit",
        @Query("precipitation_unit") precipitationUnit: String = "inch",
    ): OpenMeteoCurrentResponse
}

