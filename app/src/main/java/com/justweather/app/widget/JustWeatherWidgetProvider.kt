package com.justweather.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

                val views = RemoteViews(context.packageName, R.layout.widget_just_weather).apply {
                    setTextViewText(R.id.widget_location, settings.locationDisplay)
                    setTextViewText(
                        R.id.widget_temp,
                        weather?.tempCelsius?.let { formatTemp(it, settings.useFahrenheit) } ?: "--",
                    )
                    setTextViewText(
                        R.id.widget_humidity,
                        weather?.humidityPercent?.let { "Humidity $it%" } ?: "Humidity --",
                    )
                    setTextViewText(
                        R.id.widget_wind,
                        weather?.windSpeedMetersPerSecond?.let { "Wind ${String.format("%.1f", it)} m/s" }
                            ?: "Wind --",
                    )
                    setTextViewText(
                        R.id.widget_updated,
                        weather?.updatedAtEpochMs?.let { "Updated ${formatTime(it)}" } ?: "Updated --",
                    )
                    setTextViewText(R.id.widget_forecast, formatForecast(forecast, settings.useFahrenheit))

                    val launchIntent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                }

                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, views)
                }
            }
        }

        private fun formatTemp(tempC: Double, useF: Boolean): String {
            return if (useF) {
                String.format("%.0f°F", (tempC * 9.0 / 5.0) + 32.0)
            } else {
                String.format("%.0f°C", tempC)
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

