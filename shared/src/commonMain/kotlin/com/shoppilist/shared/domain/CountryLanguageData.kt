package com.shoppilist.shared.domain

data class Country(val code: String, val name: String, val flag: String, val languageCodes: List<String>)
data class AppLanguage(val code: String, val name: String, val nativeName: String)

/** Static onboarding reference data (§2.1) — countries named in the product spec plus their supported languages. */
object CountryLanguageData {
    val languages: List<AppLanguage> = listOf(
        AppLanguage("en", "English", "English"),
        AppLanguage("hi", "Hindi", "हिन्दी"),
        AppLanguage("ar", "Arabic", "العربية"),
        AppLanguage("es", "Spanish", "Español"),
        AppLanguage("fr", "French", "Français"),
        AppLanguage("de", "German", "Deutsch"),
        AppLanguage("pt", "Portuguese", "Português"),
        AppLanguage("in", "Bahasa Indonesia", "Bahasa Indonesia")
    )

    val countries: List<Country> = listOf(
        Country("IN", "India", "🇮🇳", listOf("en", "hi")),
        Country("US", "United States", "🇺🇸", listOf("en", "es")),
        Country("GB", "United Kingdom", "🇬🇧", listOf("en")),
        Country("AE", "United Arab Emirates", "🇦🇪", listOf("en", "ar")),
        Country("DE", "Germany", "🇩🇪", listOf("en", "de")),
        Country("AU", "Australia", "🇦🇺", listOf("en")),
        Country("SG", "Singapore", "🇸🇬", listOf("en", "in")),
        Country("BR", "Brazil", "🇧🇷", listOf("pt", "en")),
        Country("FR", "France", "🇫🇷", listOf("fr", "en"))
    )

    fun languagesFor(countryCode: String): List<AppLanguage> {
        val codes = countries.find { it.code == countryCode }?.languageCodes ?: listOf("en")
        return languages.filter { it.code in codes }
    }
}
