package com.justweather.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2A5AAA),
    onPrimary = Color.White,
    background = Color(0xFFF2F6FC),
    onBackground = Color(0xFF10223A),
    surface = Color(0xFFFAFCFF),
    onSurface = Color(0xFF12253F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CB9FF),
    onPrimary = Color(0xFF0B2B5E),
    background = Color(0xFF0F1828),
    onBackground = Color(0xFFE5EEFF),
    surface = Color(0xFF162235),
    onSurface = Color(0xFFE5EEFF),
)

@Composable
fun JustWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
