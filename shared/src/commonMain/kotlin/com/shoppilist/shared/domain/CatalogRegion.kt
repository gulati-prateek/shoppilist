package com.shoppilist.shared.domain

/**
 * Maps a user's country to the remote catalog region it should fetch. Regions are the unit the
 * Firestore catalog is authored in (`catalog/{region}/items`): dedicated catalogs for India and
 * the USA, one shared European catalog, and the bundled GLOBAL seed as the fallback for
 * everywhere else (GLOBAL has no remote counterpart — nothing to fetch).
 */
object CatalogRegion {
    const val GLOBAL = "GLOBAL"
    const val INDIA = "IN"
    const val USA = "US"
    const val EUROPE = "EU"

    /** Regions with a remote (Firestore) catalog worth syncing. */
    val REMOTE_REGIONS = listOf(INDIA, USA, EUROPE)

    private val EUROPEAN_COUNTRIES = setOf("GB", "DE", "FR", "ES", "IT", "NL", "PT", "IE", "BE", "AT", "PL")

    /**
     * [countryCode] is ISO-2, from GPS reverse-geocoding when available, else the onboarding
     * country choice — callers resolve that precedence; null lands on GLOBAL.
     */
    fun forCountry(countryCode: String?): String = when (val code = countryCode?.trim()?.uppercase()) {
        null, "" -> GLOBAL
        "IN" -> INDIA
        "US" -> USA
        in EUROPEAN_COUNTRIES -> EUROPE
        else -> GLOBAL
    }
}
