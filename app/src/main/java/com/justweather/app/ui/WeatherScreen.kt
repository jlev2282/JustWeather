package com.justweather.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.justweather.app.data.LocationSuggestion
import com.justweather.app.data.local.ForecastDayEntity
import com.justweather.app.data.local.WeatherEntity
import com.justweather.app.settings.UiSettings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherRoute(viewModel: WeatherViewModel) {
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val forecast by viewModel.forecast.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val locationInput by viewModel.locationInput.collectAsStateWithLifecycle()
    val suggestions by viewModel.locationSuggestions.collectAsStateWithLifecycle()
    val showSettings = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JustWeather") },
                actions = {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        TextButton(onClick = { viewModel.refresh() }) { Text("Refresh") }
                        TextButton(onClick = { showSettings.value = true }) { Text("Settings") }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            WeatherContent(
                weather = weather,
                forecast = forecast,
                settings = settings,
            )

            if (showSettings.value) {
                val dismissDialog = {
                    viewModel.resetLocationInput()
                    showSettings.value = false
                }
                SettingsDialog(
                    current = settings,
                    locationInput = locationInput,
                    suggestions = suggestions,
                    onLocationInputChanged = viewModel::onLocationInputChanged,
                    onSelectSuggestion = viewModel::selectSuggestedLocation,
                    onDismiss = dismissDialog,
                    onSave = { locationDisplay, useFahrenheit ->
                        viewModel.updateSettings(
                            locationDisplay = locationDisplay,
                            useFahrenheit = useFahrenheit,
                        )
                        showSettings.value = false
                    },
                )
            }
        }
    }
}

@Composable
private fun WeatherContent(
    weather: WeatherEntity?,
    forecast: List<ForecastDayEntity>,
    settings: UiSettings,
) {
    val tempText = if (weather == null) {
        "—"
    } else if (settings.useFahrenheit) {
        String.format("%.1f °F", celsiusToFahrenheit(weather.tempCelsius))
    } else {
        String.format("%.1f °C", weather.tempCelsius)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = settings.locationDisplay,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            MetricLine(label = "Temp", value = tempText)
            MetricLine(
                label = "Humidity",
                value = weather?.humidityPercent?.let { "$it%" } ?: "—",
            )
            MetricLine(
                label = "Wind Speed",
                value = weather?.windSpeedMetersPerSecond?.let { String.format("%.1f m/s", it) } ?: "—",
            )
            Text(
                text = "Last Updated",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = formatLastUpdated(weather?.updatedAtEpochMs),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Text(
                text = "7-Day Forecast",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (forecast.isEmpty()) {
                Text(
                    text = "No cached forecast yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(forecast, key = { "${it.cityQuery}-${it.dayIndex}" }) { day ->
                        ForecastCard(day = day, useFahrenheit = settings.useFahrenheit)
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastCard(day: ForecastDayEntity, useFahrenheit: Boolean) {
    val min = if (useFahrenheit) celsiusToFahrenheit(day.minTempCelsius) else day.minTempCelsius
    val max = if (useFahrenheit) celsiusToFahrenheit(day.maxTempCelsius) else day.maxTempCelsius
    val unit = if (useFahrenheit) "°F" else "°C"
    val dayName = Instant.ofEpochSecond(day.dayEpochSeconds)
        .atZone(ZoneId.systemDefault())
        .dayOfWeek
        .name
        .lowercase()
        .replaceFirstChar { it.uppercase() }
        .take(3)

    Column(
        modifier = Modifier
            .width(86.dp)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(dayName, style = MaterialTheme.typography.labelLarge)
        Text(String.format("%.0f%s", max, unit), style = MaterialTheme.typography.bodyLarge)
        Text(
            String.format("%.0f%s", min, unit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

private fun formatLastUpdated(epochMs: Long?): String {
    if (epochMs == null) return "—"
    val zoned = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    return formatter.format(zoned)
}

private fun celsiusToFahrenheit(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0

@Composable
private fun SettingsDialog(
    current: UiSettings,
    locationInput: String,
    suggestions: List<LocationSuggestion>,
    onLocationInputChanged: (String) -> Unit,
    onSelectSuggestion: (LocationSuggestion) -> Unit,
    onDismiss: () -> Unit,
    onSave: (locationDisplay: String, useFahrenheit: Boolean) -> Unit,
) {
    val useFahrenheit = remember(current.useFahrenheit) { mutableStateOf(current.useFahrenheit) }
    val canSave = locationInput.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                TextField(
                    value = locationInput,
                    onValueChange = onLocationInputChanged,
                    label = { Text("Location (City, State, Country)") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(10.dp))
                suggestions.take(5).forEach { suggestion ->
                    Text(
                        text = suggestion.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSuggestion(suggestion) }
                            .padding(vertical = 6.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Use Fahrenheit")
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = useFahrenheit.value,
                        onCheckedChange = { useFahrenheit.value = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(locationInput.trim(), useFahrenheit.value)
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MetricLine(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}
