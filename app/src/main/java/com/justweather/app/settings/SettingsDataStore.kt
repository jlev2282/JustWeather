package com.justweather.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

private val LOCATION_DISPLAY_KEY = stringPreferencesKey("location_display")
private val USE_FAHRENHEIT_KEY = booleanPreferencesKey("use_fahrenheit")

class SettingsDataStore(
    private val context: Context,
) {

    val settingsFlow: Flow<UiSettings> = context.dataStore.data.map { prefs ->
        val storedLocation = prefs[LOCATION_DISPLAY_KEY].orEmpty().trim()
        UiSettings(
            locationDisplay = if (storedLocation.isBlank()) DEFAULT_LOCATION else storedLocation,
            useFahrenheit = prefs[USE_FAHRENHEIT_KEY] ?: false,
        )
    }

    suspend fun updateSettings(locationDisplay: String, useFahrenheit: Boolean) {
        val normalizedLocation = locationDisplay.trim().ifBlank { DEFAULT_LOCATION }
        context.dataStore.edit { prefs ->
            prefs[LOCATION_DISPLAY_KEY] = normalizedLocation
            prefs[USE_FAHRENHEIT_KEY] = useFahrenheit
        }
    }

    companion object {
        private const val DEFAULT_LOCATION = "London, England, GB"
    }
}

