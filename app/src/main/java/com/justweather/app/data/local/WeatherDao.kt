package com.justweather.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather_cache WHERE cityQuery = :cityQuery LIMIT 1")
    fun observeWeather(cityQuery: String): Flow<WeatherEntity?>

    @Query("SELECT * FROM weather_cache WHERE cityQuery = :cityQuery LIMIT 1")
    suspend fun getWeather(cityQuery: String): WeatherEntity?

    @Upsert
    suspend fun upsert(entity: WeatherEntity)

    @Query("SELECT * FROM forecast_cache WHERE cityQuery = :cityQuery ORDER BY dayIndex ASC")
    fun observeForecast(cityQuery: String): Flow<List<ForecastDayEntity>>

    @Query("DELETE FROM forecast_cache WHERE cityQuery = :cityQuery")
    suspend fun deleteForecast(cityQuery: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecast(days: List<ForecastDayEntity>)

    @Transaction
    suspend fun replaceForecast(cityQuery: String, days: List<ForecastDayEntity>) {
        deleteForecast(cityQuery)
        if (days.isNotEmpty()) {
            insertForecast(days)
        }
    }
}
