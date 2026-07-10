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

    /** The 19 countries covered by the bundled CSV catalog (ShoppiList_Item_Catalog.csv), plus
     *  everywhere else falling back to the GLOBAL catalog. Not stored pre-sorted — pickers sort
     *  alphabetically by [Country.name] themselves (see CountryPickerList). */
    val countries: List<Country> = listOf(
        Country("IN", "India", "🇮🇳", listOf("en", "hi")),
        Country("US", "United States", "🇺🇸", listOf("en", "es")),
        Country("GB", "United Kingdom", "🇬🇧", listOf("en")),
        Country("AE", "United Arab Emirates", "🇦🇪", listOf("en", "ar")),
        Country("DE", "Germany", "🇩🇪", listOf("en", "de")),
        Country("AU", "Australia", "🇦🇺", listOf("en")),
        Country("SG", "Singapore", "🇸🇬", listOf("en", "in")),
        Country("BR", "Brazil", "🇧🇷", listOf("pt", "en")),
        Country("FR", "France", "🇫🇷", listOf("fr", "en")),
        Country("CN", "China", "🇨🇳", listOf("en")),
        Country("ID", "Indonesia", "🇮🇩", listOf("in", "en")),
        Country("IT", "Italy", "🇮🇹", listOf("en")),
        Country("JP", "Japan", "🇯🇵", listOf("en")),
        Country("MX", "Mexico", "🇲🇽", listOf("es", "en")),
        Country("NG", "Nigeria", "🇳🇬", listOf("en")),
        Country("SA", "Saudi Arabia", "🇸🇦", listOf("ar", "en")),
        Country("ZA", "South Africa", "🇿🇦", listOf("en")),
        Country("KR", "South Korea", "🇰🇷", listOf("en")),
        Country("ES", "Spain", "🇪🇸", listOf("es", "en"))
    )

    fun languagesFor(countryCode: String): List<AppLanguage> {
        val codes = countries.find { it.code == countryCode }?.languageCodes ?: listOf("en")
        return languages.filter { it.code in codes }
    }
}
