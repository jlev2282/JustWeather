package com.justweather.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.justweather.app.MainActivity
import com.justweather.app.R
import com.justweather.app.data.local.ForecastDayEntity
import com.justweather.app.data.local.WeatherDatabase
import com.justweather.app.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class JustWeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        refreshAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        refreshAllWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    companion object {
        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, JustWeatherWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            refreshAllWidgets(context, manager, ids)
        }

        private fun refreshAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
        ) {
            if (appWidgetIds.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                val dao = WeatherDatabase.getInstance(context).weatherDao()
                val settings = SettingsDataStore(context).getCurrentSettings()
                val weather = dao.getWeather(settings.locationDisplay)
                val forecast = dao.getForecast(settings.locationDisplay).take(3)
                appWidgetIds.forEach { id ->
                    val options = appWidgetManager.getAppWidgetOptions(id)
                    val views = if (isWideWidget(options)) {
                    buildWideViews(context, settings.locationDisplay, weather, forecast)
                    } else {
                        buildCompactViews(context, settings.locationDisplay, weather, forecast, settings.useFahrenheit)
                    }
                    appWidgetManager.updateAppWidget(id, views)
                }
            }
        }

        private fun buildCompactViews(
            context: Context,
            locationDisplay: String,
            weather: com.justweather.app.data.local.WeatherEntity?,
            forecast: List<ForecastDayEntity>,
            useFahrenheit: Boolean,
        ): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_just_weather).apply {
                val backgroundArt = conditionBackgroundArtRes(weather?.weatherCode, weather?.windSpeedMetersPerSecond)
                val backgroundScene = conditionBackgroundSceneRes(weather?.weatherCode, weather?.windSpeedMetersPerSecond)
                setImageViewResource(R.id.widget_watermark_icon, backgroundArt)
                setInt(R.id.widget_root, "setBackgroundResource", backgroundScene)
                setTextViewText(R.id.widget_location, locationDisplay)
                setTextViewText(
                    R.id.widget_temp,
                    weather?.tempCelsius?.let { formatTemp(it, useFahrenheit) } ?: "--",
                )
                setTextViewText(
                    R.id.widget_humidity,
                    weather?.humidityPercent?.let { "Humidity $it%" } ?: "Humidity --",
                )
                setTextViewText(
                    R.id.widget_wind,
                    weather?.windSpeedMetersPerSecond?.let { "Wind ${formatWind(it, useFahrenheit)}" }
                        ?: "Wind --",
                )
                setTextViewText(
                    R.id.widget_updated,
                    weather?.updatedAtEpochMs?.let { "Updated ${formatTime(it)}" } ?: "Updated --",
                )
                setTextViewText(R.id.widget_forecast, formatForecast(forecast, useFahrenheit))
                setOnClickPendingIntent(R.id.widget_root, launchPendingIntent(context))
            }
        }

        private fun buildWideViews(
            context: Context,
            locationDisplay: String,
            weather: com.justweather.app.data.local.WeatherEntity?,
            forecast: List<ForecastDayEntity>,
        ): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_just_weather_wide).apply {
                val backgroundArt = conditionBackgroundArtRes(weather?.weatherCode, weather?.windSpeedMetersPerSecond)
                val backgroundScene = conditionBackgroundSceneRes(weather?.weatherCode, weather?.windSpeedMetersPerSecond)
                setImageViewResource(R.id.widget_wide_watermark_icon, backgroundArt)
                setInt(R.id.widget_wide_root, "setBackgroundResource", backgroundScene)
                setTextViewText(R.id.widget_wide_location, locationDisplay)
                setTextViewText(
                    R.id.widget_wide_temp,
                    weather?.tempCelsius?.let { String.format("%.0f°F", (it * 9.0 / 5.0) + 32.0) } ?: "--",
                )
                val highLow = forecast.firstOrNull()?.let {
                    val high = (it.maxTempCelsius * 9.0 / 5.0) + 32.0
                    val low = (it.minTempCelsius * 9.0 / 5.0) + 32.0
                    "H:${String.format("%.0f", high)}°  L:${String.format("%.0f", low)}°"
                } ?: "H:--  L:--"
                setTextViewText(R.id.widget_wide_high_low, highLow)
                setOnClickPendingIntent(R.id.widget_wide_root, launchPendingIntent(context))
            }
        }

        private fun launchPendingIntent(context: Context): PendingIntent {
            val launchIntent = Intent(context, MainActivity::class.java)
            return PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun isWideWidget(options: Bundle): Boolean {
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            return minWidth >= 280 && minHeight <= 130
        }

        private fun formatTemp(tempC: Double, useF: Boolean): String {
            return if (useF) {
                String.format("%.0f°F", (tempC * 9.0 / 5.0) + 32.0)
            } else {
                String.format("%.0f°C", tempC)
            }
        }

        private fun weatherIconRes(weatherCode: Int?): Int {
            if (weatherCode == null) return R.drawable.ic_weather_unknown
            return when (weatherCode) {
                0 -> R.drawable.ic_weather_clear
                1, 2, 3 -> R.drawable.ic_weather_cloudy
                45, 48 -> R.drawable.ic_weather_fog
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.ic_weather_rain
                71, 73, 75, 77, 85, 86 -> R.drawable.ic_weather_snow
                95, 96, 99 -> R.drawable.ic_weather_storm
                else -> R.drawable.ic_weather_unknown
            }
        }

        private fun conditionIconRes(weatherCode: Int?, windSpeedMetersPerSecond: Double?): Int {
            if ((windSpeedMetersPerSecond ?: 0.0) >= 10.0) {
                return R.drawable.ic_weather_windy
            }
            return weatherIconRes(weatherCode)
        }

        private fun conditionBackgroundArtRes(weatherCode: Int?, windSpeedMetersPerSecond: Double?): Int {
            if ((windSpeedMetersPerSecond ?: 0.0) >= 10.0) {
                return R.drawable.ic_weather_windy
            }
            return when (weatherCode) {
                0 -> R.drawable.ic_weather_clear
                1, 2, 3 -> R.drawable.ic_weather_cloudy
                45, 48 -> R.drawable.ic_weather_fog
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.ic_weather_rain
                71, 73, 75, 77, 85, 86 -> R.drawable.ic_weather_snow
                95, 96, 99 -> R.drawable.ic_weather_storm
                else -> R.drawable.ic_weather_unknown
            }
        }

        private fun conditionBackgroundSceneRes(weatherCode: Int?, windSpeedMetersPerSecond: Double?): Int {
            if ((windSpeedMetersPerSecond ?: 0.0) >= 10.0) return R.drawable.widget_scene_windy
            return when (weatherCode) {
                0 -> R.drawable.widget_scene_clear
                1, 2, 3 -> R.drawable.widget_scene_cloudy
                45, 48 -> R.drawable.widget_scene_fog
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.widget_scene_rain
                71, 73, 75, 77, 85, 86 -> R.drawable.widget_scene_snow
                95, 96, 99 -> R.drawable.widget_scene_storm
                else -> R.drawable.widget_scene_unknown
            }
        }

        private fun formatWind(metersPerSecond: Double, useFahrenheit: Boolean): String {
            return if (useFahrenheit) {
                String.format("%.1f mph", metersPerSecond * 2.23694)
            } else {
                String.format("%.1f m/s", metersPerSecond)
            }
        }

        private fun formatForecast(days: List<ForecastDayEntity>, useF: Boolean): String {
            if (days.isEmpty()) return "Forecast unavailable"
            val dayFormatter = DateTimeFormatter.ofPattern("EEE")
            return days.joinToString("  ") { day ->
                val label = Instant.ofEpochSecond(day.dayEpochSeconds)
                    .atZone(ZoneId.systemDefault())
                    .format(dayFormatter)
                val max = if (useF) (day.maxTempCelsius * 9.0 / 5.0) + 32.0 else day.maxTempCelsius
                val min = if (useF) (day.minTempCelsius * 9.0 / 5.0) + 32.0 else day.minTempCelsius
                val unit = if (useF) "F" else "C"
                "$label ${String.format("%.0f", max)}/${String.format("%.0f", min)}$unit"
            }
        }

        private fun formatTime(epochMs: Long): String {
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            return Instant.ofEpochMilli(epochMs)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        }
    }
}

