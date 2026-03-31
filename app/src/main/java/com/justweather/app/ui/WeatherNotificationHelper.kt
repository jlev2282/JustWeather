package com.justweather.app.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.justweather.app.R
import com.justweather.app.data.SevereAlert

object WeatherNotificationHelper {

    private const val CHANNEL_ID = "severe_weather_alerts"

    fun showSevereAlert(context: Context, alert: SevereAlert) {
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(alert.title.hashCode(), notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Severe Weather Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts when severe weather conditions are detected."
        }
        manager.createNotificationChannel(channel)
    }
}

