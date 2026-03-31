package com.justweather.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApi {

    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") cityQuery: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
    ): OpenWeatherResponse

    @GET("data/3.0/onecall")
    suspend fun getSevenDayForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("exclude") exclude: String = "current,minutely,hourly,alerts",
        @Query("units") units: String = "metric",
    ): ForecastResponse

    @GET("geo/1.0/direct")
    suspend fun geocodeLocations(
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("appid") apiKey: String,
    ): List<GeoLocationResponse>
}
