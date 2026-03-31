package com.justweather.app.data

data class LocationSuggestion(
    val city: String,
    val state: String?,
    val country: String,
    val lat: Double,
    val lon: Double,
) {
    val displayName: String
        get() = listOfNotNull(
            city.takeIf { it.isNotBlank() },
            state?.takeIf { it.isNotBlank() },
            country.takeIf { it.isNotBlank() },
        ).joinToString(", ")
}

