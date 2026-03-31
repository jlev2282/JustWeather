package com.justweather.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.justweather.app.data.WeatherRepository
import com.justweather.app.data.local.WeatherDatabase
import com.justweather.app.settings.SettingsDataStore
import com.justweather.app.ui.WeatherRoute
import com.justweather.app.ui.WeatherViewModel
import com.justweather.app.ui.WeatherViewModelFactory
import com.justweather.app.ui.theme.JustWeatherTheme
import com.justweather.app.widget.JustWeatherWidgetProvider

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = WeatherDatabase.getInstance(applicationContext)
        val repository = WeatherRepository(database.weatherDao())
        val settingsDataStore = SettingsDataStore(applicationContext)
        val factory = WeatherViewModelFactory(repository, settingsDataStore)

        setContent {
            JustWeatherTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: WeatherViewModel = viewModel(factory = factory)
                    WeatherRoute(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        JustWeatherWidgetProvider.refreshAllWidgets(this)
    }
}
