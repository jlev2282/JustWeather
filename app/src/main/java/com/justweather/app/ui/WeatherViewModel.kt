package com.justweather.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justweather.app.data.LocationSuggestion
import com.justweather.app.data.WeatherRepository
import com.justweather.app.data.local.ForecastDayEntity
import com.justweather.app.data.local.WeatherEntity
import com.justweather.app.settings.SettingsDataStore
import com.justweather.app.settings.UiSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val settings: StateFlow<UiSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiSettings(locationDisplay = DEFAULT_LOCATION, useFahrenheit = false),
        )

    val weather: StateFlow<WeatherEntity?> = settingsDataStore.settingsFlow
        .flatMapLatest { uiSettings ->
            repository.observeWeather(cityQuery = uiSettings.locationDisplay)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val forecast: StateFlow<List<ForecastDayEntity>> = settingsDataStore.settingsFlow
        .flatMapLatest { uiSettings ->
            repository.observeForecast(cityQuery = uiSettings.locationDisplay)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _locationInput = MutableStateFlow(DEFAULT_LOCATION)
    val locationInput: StateFlow<String> = _locationInput.asStateFlow()

    private val _locationSuggestions = MutableStateFlow<List<LocationSuggestion>>(emptyList())
    val locationSuggestions: StateFlow<List<LocationSuggestion>> = _locationSuggestions.asStateFlow()

    init {
        // Refresh on location changes. Switching units should not trigger network calls.
        viewModelScope.launch {
            settingsDataStore.settingsFlow
                .map { it.locationDisplay }
                .distinctUntilChanged()
                .collect { location ->
                    _locationInput.value = location
                    refreshInternal(location)
                }
        }

        viewModelScope.launch {
            _locationInput
                .debounce(300)
                .distinctUntilChanged()
                .collect { input ->
                    _locationSuggestions.value = repository.searchLocations(input)
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshInternal(settings.value.locationDisplay)
        }
    }

    fun onLocationInputChanged(input: String) {
        _locationInput.value = input
    }

    fun selectSuggestedLocation(suggestion: LocationSuggestion) {
        _locationInput.value = suggestion.displayName
    }

    fun resetLocationInput() {
        _locationInput.value = settings.value.locationDisplay
    }

    fun updateSettings(locationDisplay: String, useFahrenheit: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateSettings(
                locationDisplay = locationDisplay,
                useFahrenheit = useFahrenheit,
            )
        }
    }

    private suspend fun refreshInternal(cityQuery: String) {
        _isRefreshing.value = true
        try {
            repository.refreshWeather(cityQuery = cityQuery)
        } finally {
            _isRefreshing.value = false
        }
    }

    companion object {
        private const val DEFAULT_LOCATION = "London, England, GB"
    }
}

class WeatherViewModelFactory(
    private val repository: WeatherRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return WeatherViewModel(repository, settingsDataStore) as T
    }
}
