package com.moham.taxi.utils

import java.util.Locale

object CurrencyUtils {
    private val euCountries = setOf(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT", 
        "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    )

    fun isEuCountry(locale: Locale = Locale.getDefault()): Boolean {
        return euCountries.contains(locale.country.uppercase())
    }

    fun getCurrencySymbol(locale: Locale = Locale.getDefault()): String {
        return if (isEuCountry(locale)) "€" else "$"
    }
}
