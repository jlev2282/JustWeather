package com.justweather.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    val currentCondition = resolveCondition(weather)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        AnimatedWeatherScene(
            sceneType = currentCondition.sceneType,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.28f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
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
            MetricLine(label = "Temp", value = tempText, modifier = Modifier.padding(top = 8.dp))
            MetricLine(
                label = "Humidity",
                value = weather?.humidityPercent?.let { "$it%" } ?: "—",
            )
            MetricLine(
                label = "Wind Speed",
                value = weather?.windSpeedMetersPerSecond?.let { formatWindSpeed(it, settings.useFahrenheit) } ?: "—",
            )
            MetricLine(
                label = "Last Updated",
                value = formatLastUpdated(weather?.updatedAtEpochMs),
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
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(forecast, key = { "${it.cityQuery}-${it.dayIndex}" }) { day ->
                            ForecastCard(day = day, useFahrenheit = settings.useFahrenheit)
                        }
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(String.format("%.0f%s", max, unit), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                String.format("%.0f%s", min, unit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Image(
            painter = painterResource(id = forecastIconRes(day.weatherCode)),
            contentDescription = "Forecast icon for $dayName",
            modifier = Modifier
                .width(22.dp)
                .height(22.dp),
        )
    }
}

private fun forecastIconRes(weatherCode: Int?): Int {
    return when (weatherCode) {
        0 -> com.justweather.app.R.drawable.ic_weather_clear
        1, 2, 3 -> com.justweather.app.R.drawable.ic_weather_cloudy
        45, 48 -> com.justweather.app.R.drawable.ic_weather_fog
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> com.justweather.app.R.drawable.ic_weather_rain
        71, 73, 75, 77, 85, 86 -> com.justweather.app.R.drawable.ic_weather_snow
        95, 96, 99 -> com.justweather.app.R.drawable.ic_weather_storm
        else -> com.justweather.app.R.drawable.ic_weather_unknown
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
    val sceneType: WeatherSceneType,
)

private enum class WeatherSceneType {
    CLEAR, CLOUDY, FOG, RAIN, SNOW, STORM, WINDY, UNKNOWN
}

private fun resolveCondition(weather: WeatherEntity?): WeatherConditionUi {
    if (weather == null) {
        return WeatherConditionUi(
            label = "Unknown",
            icon = Icons.Default.FilterDrama,
            backgroundColor = Color(0xFFF2F6FC),
            sceneType = WeatherSceneType.UNKNOWN,
        )
    }

    if (weather.windSpeedMetersPerSecond >= 10.0) {
        return WeatherConditionUi(
            label = "Windy",
            icon = Icons.Default.Cloud,
            backgroundColor = Color(0xFFE3EAF3),
            sceneType = WeatherSceneType.WINDY,
        )
    }

    return decodeWeatherCode(weather.weatherCode)
}

private fun decodeWeatherCode(code: Int): WeatherConditionUi {
    return when (code) {
        0 -> WeatherConditionUi("Clear Sky", Icons.Default.WbSunny, Color(0xFFE8F4FF), WeatherSceneType.CLEAR)
        1, 2 -> WeatherConditionUi("Partly Cloudy", Icons.Default.Cloud, Color(0xFFEFF4FA), WeatherSceneType.CLOUDY)
        3 -> WeatherConditionUi("Overcast", Icons.Default.Cloud, Color(0xFFE1E5EC), WeatherSceneType.CLOUDY)
        45, 48 -> WeatherConditionUi("Foggy", Icons.Default.FilterDrama, Color(0xFFE7EAF0), WeatherSceneType.FOG)
        51, 53, 55, 56, 57 -> WeatherConditionUi("Drizzle", Icons.Default.Grain, Color(0xFFE5EDF7), WeatherSceneType.RAIN)
        61 -> WeatherConditionUi("Slight Rain", Icons.Default.Grain, Color(0xFFDDE9F7), WeatherSceneType.RAIN)
        63, 65, 66, 67 -> WeatherConditionUi("Rain", Icons.Default.Grain, Color(0xFFD7E5F6), WeatherSceneType.RAIN)
        71, 73, 75, 77 -> WeatherConditionUi("Snow", Icons.Default.Cloud, Color(0xFFEAF1FA), WeatherSceneType.SNOW)
        80, 81, 82 -> WeatherConditionUi("Rain Showers", Icons.Default.Grain, Color(0xFFD8E6F8), WeatherSceneType.RAIN)
        85, 86 -> WeatherConditionUi("Snow Showers", Icons.Default.Cloud, Color(0xFFEAF1FA), WeatherSceneType.SNOW)
        95 -> WeatherConditionUi("Thunderstorm", Icons.Default.Thunderstorm, Color(0xFF312E5A), WeatherSceneType.STORM)
        96, 99 -> WeatherConditionUi("Stormy", Icons.Default.Bolt, Color(0xFF2B2956), WeatherSceneType.STORM)
        else -> WeatherConditionUi("Unknown", Icons.Default.FilterDrama, Color(0xFFF2F6FC), WeatherSceneType.UNKNOWN)
    }
}

@Composable
private fun AnimatedWeatherScene(sceneType: WeatherSceneType, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "weather-scene")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000), RepeatMode.Reverse),
        label = "drift",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (sceneType) {
            WeatherSceneType.CLEAR -> {
                drawCircle(Color(0xFFFFD36E), radius = h * 0.22f, center = Offset(w * (0.75f + 0.05f * drift), h * 0.25f))
            }
            WeatherSceneType.CLOUDY, WeatherSceneType.FOG, WeatherSceneType.UNKNOWN -> {
                drawCircle(Color(0xFFB7C3D6), radius = h * 0.22f, center = Offset(w * (0.35f + 0.08f * drift), h * 0.32f))
                drawCircle(Color(0xFFA4B2C8), radius = h * 0.2f, center = Offset(w * (0.58f + 0.06f * drift), h * 0.35f))
            }
            WeatherSceneType.RAIN -> {
                for (i in 0..14) {
                    val x = w * (i / 14f)
                    val y = h * (0.18f + (i % 4) * 0.08f + 0.05f * drift)
                    drawLine(Color(0xFF74A7D8), Offset(x, y), Offset(x - 14f, y + 32f), strokeWidth = 6f)
                }
            }
            WeatherSceneType.SNOW -> {
                for (i in 0..24) {
                    val x = w * (i / 24f)
                    val y = h * (0.15f + (i % 5) * 0.1f + 0.04f * drift)
                    drawCircle(Color(0xFFEAF5FF), radius = 7f, center = Offset(x, y))
                }
            }
            WeatherSceneType.STORM -> {
                drawCircle(Color(0xFF2B2F67), radius = h * 0.25f, center = Offset(w * 0.5f, h * 0.3f))
                drawLine(Color(0xFFFFE082), Offset(w * 0.57f, h * 0.28f), Offset(w * 0.47f, h * 0.6f), strokeWidth = 14f)
                drawLine(Color(0xFFFFE082), Offset(w * 0.47f, h * 0.6f), Offset(w * 0.6f, h * 0.58f), strokeWidth = 14f)
            }
            WeatherSceneType.WINDY -> {
                for (i in 0..5) {
                    val y = h * (0.18f + i * 0.12f)
                    drawArc(
                        color = Color(0xFFA6B9D6),
                        startAngle = 12f,
                        sweepAngle = 220f,
                        useCenter = false,
                        topLeft = Offset(w * (0.08f + 0.1f * drift), y),
                        size = Size(w * 0.72f, h * 0.22f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 9f),
                    )
                }
            }
        }
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
private fun MetricLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Start,
        )
    }
}
