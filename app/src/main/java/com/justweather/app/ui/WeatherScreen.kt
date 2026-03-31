package com.justweather.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.collect
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
    val context = LocalContext.current
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val forecast by viewModel.forecast.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val locationInput by viewModel.locationInput.collectAsStateWithLifecycle()
    val suggestions by viewModel.locationSuggestions.collectAsStateWithLifecycle()
    val showSettings = remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.severeAlertEvents.collect { alert ->
            WeatherNotificationHelper.showSevereAlert(context, alert)
        }
    }

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
                    onSave = { locationDisplay, useFahrenheit, severeEnabled ->
                        viewModel.updateSettings(
                            locationDisplay = locationDisplay,
                            useFahrenheit = useFahrenheit,
                            severeNotificationsEnabled = severeEnabled,
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
    val currentCondition = weather?.weatherCode?.let(::decodeWeatherCode)
        ?: WeatherConditionUi("Unknown", Icons.Default.FilterDrama, Color(0xFFF2F6FC))
    val backgroundColor by animateColorAsState(
        targetValue = currentCondition.backgroundColor,
        label = "weather-bg",
    )

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
            .background(backgroundColor)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Current Conditions",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Icon(
                    imageVector = currentCondition.icon,
                    contentDescription = currentCondition.label,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = currentCondition.label,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = weather?.let { formatFeelsLike(it.feelsLikeFahrenheit, settings.useFahrenheit) } ?: "Feels Like —",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            Text(
                text = settings.locationDisplay,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
            )
            MetricLine(label = "Temp", value = tempText)
            MetricLine(
                label = "Humidity",
                value = weather?.humidityPercent?.let { "$it%" } ?: "—",
            )
            MetricLine(
                label = "Wind Speed",
                value = weather?.windSpeedMetersPerSecond?.let { formatWindSpeed(it, settings.useFahrenheit) } ?: "—",
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
private fun fahrenheitToCelsius(f: Double): Double = (f - 32.0) * 5.0 / 9.0

private fun metersPerSecondToMph(mps: Double): Double = mps * 2.23694

private fun formatWindSpeed(mps: Double, useFahrenheit: Boolean): String {
    return if (useFahrenheit) {
        String.format("%.1f mph", metersPerSecondToMph(mps))
    } else {
        String.format("%.1f m/s", mps)
    }
}

private fun formatFeelsLike(feelsLikeF: Double, useFahrenheit: Boolean): String {
    return if (useFahrenheit) {
        "Feels Like ${String.format("%.1f °F", feelsLikeF)}"
    } else {
        "Feels Like ${String.format("%.1f °C", fahrenheitToCelsius(feelsLikeF))}"
    }
}

private data class WeatherConditionUi(
    val label: String,
    val icon: ImageVector,
    val backgroundColor: Color,
)

private fun decodeWeatherCode(code: Int): WeatherConditionUi {
    return when (code) {
        0 -> WeatherConditionUi("Clear Sky", Icons.Default.WbSunny, Color(0xFFE8F4FF))
        1, 2 -> WeatherConditionUi("Partly Cloudy", Icons.Default.Cloud, Color(0xFFEFF4FA))
        3 -> WeatherConditionUi("Overcast", Icons.Default.Cloud, Color(0xFFE1E5EC))
        45, 48 -> WeatherConditionUi("Foggy", Icons.Default.FilterDrama, Color(0xFFE7EAF0))
        51, 53, 55, 56, 57 -> WeatherConditionUi("Drizzle", Icons.Default.Grain, Color(0xFFE5EDF7))
        61 -> WeatherConditionUi("Slight Rain", Icons.Default.Grain, Color(0xFFDDE9F7))
        63, 65, 66, 67 -> WeatherConditionUi("Rain", Icons.Default.Grain, Color(0xFFD7E5F6))
        71, 73, 75, 77 -> WeatherConditionUi("Snow", Icons.Default.Cloud, Color(0xFFEAF1FA))
        80, 81, 82 -> WeatherConditionUi("Rain Showers", Icons.Default.Grain, Color(0xFFD8E6F8))
        85, 86 -> WeatherConditionUi("Snow Showers", Icons.Default.Cloud, Color(0xFFEAF1FA))
        95 -> WeatherConditionUi("Thunderstorm", Icons.Default.Thunderstorm, Color(0xFF312E5A))
        96, 99 -> WeatherConditionUi("Stormy", Icons.Default.Bolt, Color(0xFF2B2956))
        else -> WeatherConditionUi("Unknown", Icons.Default.FilterDrama, Color(0xFFF2F6FC))
    }
}

@Composable
private fun SettingsDialog(
    current: UiSettings,
    locationInput: String,
    suggestions: List<LocationSuggestion>,
    onLocationInputChanged: (String) -> Unit,
    onSelectSuggestion: (LocationSuggestion) -> Unit,
    onDismiss: () -> Unit,
    onSave: (locationDisplay: String, useFahrenheit: Boolean, severeNotificationsEnabled: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val useFahrenheit = remember(current.useFahrenheit) { mutableStateOf(current.useFahrenheit) }
    val severeNotificationsEnabled = remember(current.severeNotificationsEnabled) {
        mutableStateOf(current.severeNotificationsEnabled)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        severeNotificationsEnabled.value = granted
    }
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
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Severe Weather Notifications")
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = severeNotificationsEnabled.value,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                severeNotificationsEnabled.value = false
                            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                severeNotificationsEnabled.value = true
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    severeNotificationsEnabled.value = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        locationInput.trim(),
                        useFahrenheit.value,
                        severeNotificationsEnabled.value,
                    )
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
