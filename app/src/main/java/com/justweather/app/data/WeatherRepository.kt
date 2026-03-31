package com.justweather.app.data

import com.justweather.app.BuildConfig
import com.justweather.app.data.local.ForecastDayEntity
import com.justweather.app.data.local.WeatherDao
import com.justweather.app.data.local.WeatherEntity
import com.justweather.app.data.remote.GeoLocationResponse
import com.justweather.app.data.remote.NetworkModule
import com.justweather.app.data.remote.OpenMeteoApi
import com.justweather.app.data.remote.OpenWeatherApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WeatherRepository(
    private val weatherDao: WeatherDao,
    private val api: OpenWeatherApi = NetworkModule.openWeatherApi,
    private val openMeteoApi: OpenMeteoApi = NetworkModule.openMeteoApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /**
     * Emits cached [WeatherEntity] from Room immediately (including `null` if empty),
     * then emits again after a successful background refresh from the API.
     */
    fun observeWeather(cityQuery: String): Flow<WeatherEntity?> =
        weatherDao.observeWeather(cityQuery)

    fun observeForecast(cityQuery: String): Flow<List<ForecastDayEntity>> =
        weatherDao.observeForecast(cityQuery)

    /**
     * Fetches fresh weather in the background and upserts into Room. Cached rows stay
     * visible if the network call fails.
     */
    suspend fun refreshWeather(cityQuery: String = DEFAULT_CITY) {
        if (BuildConfig.WEATHER_API_KEY.isBlank()) return
        val query = cityQuery.trim()
        if (query.isBlank()) return
        withContext(ioDispatcher) {
            runCatching {
                val remote = api.getCurrentWeather(query, BuildConfig.WEATHER_API_KEY)
                val now = System.currentTimeMillis()
                weatherDao.upsert(
                    WeatherEntity(
                        cityQuery = query,
                        cityName = remote.name,
                        latitude = remote.coord.lat,
                        longitude = remote.coord.lon,
                        tempCelsius = remote.main.temp,
                        humidityPercent = remote.main.humidity,
                        windSpeedMetersPerSecond = remote.wind.speed,
                        updatedAtEpochMs = now,
                    ),
                )

                val forecast = api.getSevenDayForecast(
                    lat = remote.coord.lat,
                    lon = remote.coord.lon,
                    apiKey = BuildConfig.WEATHER_API_KEY,
                )
                val forecastRows = forecast.daily
                    .take(7)
                    .mapIndexed { index, day ->
                        ForecastDayEntity(
                            cityQuery = query,
                            dayIndex = index,
                            dayEpochSeconds = day.dt,
                            minTempCelsius = day.temp.min,
                            maxTempCelsius = day.temp.max,
                        )
                    }
                weatherDao.replaceForecast(query, forecastRows)
            }
        }
    }

    suspend fun checkSevereConditions(cityQuery: String): SevereAlert? {
        val query = cityQuery.trim()
        if (query.isBlank()) return null
        return withContext(ioDispatcher) {
            val cached = weatherDao.getWeather(query) ?: return@withContext null
            val current = runCatching {
                openMeteoApi.getCurrentConditions(
                    latitude = cached.latitude,
                    longitude = cached.longitude,
                ).current
            }.getOrNull() ?: return@withContext null

            when {
                current.temperature_2m > 95.0 -> {
                    SevereAlert(
                        title = "Heat Advisory",
                        message = "Current temperature is ${current.temperature_2m.toInt()}°F near ${cached.cityName}.",
                    )
                }
                current.precipitation > 0.5 -> {
                    SevereAlert(
                        title = "Heavy Rain Warning",
                        message = "Precipitation is ${String.format("%.2f", current.precipitation)} in/hr near ${cached.cityName}.",
                    )
                }
                current.weather_code in THUNDERSTORM_CODES -> {
                    SevereAlert(
                        title = "Thunderstorm Alert",
                        message = "Thunderstorm conditions detected near ${cached.cityName}.",
                    )
                }
                else -> null
            }
        }
    }

    suspend fun searchLocations(query: String, limit: Int = 6): List<LocationSuggestion> {
        if (BuildConfig.WEATHER_API_KEY.isBlank()) return emptyList()
        val normalized = query.trim()
        if (normalized.length < 2) return emptyList()
        return withContext(ioDispatcher) {
            runCatching {
                api.geocodeLocations(
                    query = normalized,
                    limit = limit,
                    apiKey = BuildConfig.WEATHER_API_KEY,
                ).map(::toLocationSuggestion)
            }.getOrDefault(emptyList())
        }
    }

    private fun toLocationSuggestion(raw: GeoLocationResponse): LocationSuggestion {
        return LocationSuggestion(
            city = raw.name,
            state = raw.state,
            country = raw.country,
            lat = raw.lat,
            lon = raw.lon,
        )
    }

    companion object {
        private const val DEFAULT_CITY = "London"
        private val THUNDERSTORM_CODES = setOf(95, 96, 99)
    }
}
