package com.shoppilist.shared.data.session

import com.russhwolf.settings.Settings
import com.shoppilist.shared.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the logged-in user's identity. There's no real auth backend in this project, so
 * Login/Register just upsert a UserEntity and record its id here — everything that needs
 * "who am I" (assignment, attribution, history, invites) reads through this instead of the
 * hardcoded "current_user_id" placeholders the scaffold used to have.
 *
 * Backed by [Settings] (multiplatform-settings) rather than `SharedPreferences` directly, since
 * `SharedPreferences` has no Kotlin/Native equivalent — the platform-specific [Settings] instance
 * (`SharedPreferencesSettings` on Android, `NSUserDefaultsSettings` on iOS) is constructed by
 * each platform's own DI setup and passed in here.
 */
class SessionManager(private val settings: Settings) {
    private val _currentUserId = MutableStateFlow(settings.getStringOrNull(KEY_USER_ID))
    val currentUserId: StateFlow<String?> = _currentUserId

    fun requireUserId(): String = _currentUserId.value ?: FALLBACK_USER_ID

    fun setCurrentUser(userId: String) {
        settings.putString(KEY_USER_ID, userId)
        _currentUserId.value = userId
    }

    fun clear() {
        settings.remove(KEY_USER_ID)
        _currentUserId.value = null
    }

    /** Country/language picked during onboarding, before a user account exists yet — applied at login/register. */
    fun setPendingLocale(countryCode: String, languageCode: String) {
        settings.putString(KEY_PENDING_COUNTRY, countryCode)
        settings.putString(KEY_PENDING_LANGUAGE, languageCode)
    }

    fun consumePendingLocale(): Pair<String, String>? {
        val country = settings.getStringOrNull(KEY_PENDING_COUNTRY)
        val language = settings.getStringOrNull(KEY_PENDING_LANGUAGE)
        if (country == null || language == null) return null
        settings.remove(KEY_PENDING_COUNTRY)
        settings.remove(KEY_PENDING_LANGUAGE)
        return country to language
    }

    /** Whether the intro/locale onboarding has been completed once on this device — relaunches
     *  skip straight to Login instead of replaying the whole intro. */
    var onboardingDone: Boolean
        get() = settings.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) { settings.putBoolean(KEY_ONBOARDING_DONE, value) }

    /** ISO country code the user last selected for the phone-number field — remembered across
     *  sessions so the phone country code defaults to the one they chose at first use. */
    var phoneCountryCode: String?
        get() = settings.getStringOrNull(KEY_PHONE_COUNTRY)
        set(value) { if (value == null) settings.remove(KEY_PHONE_COUNTRY) else settings.putString(KEY_PHONE_COUNTRY, value) }

    /** Last device location the user fetched (dashboard chip). Survives logout deliberately —
     *  [clear] leaves it so the value is remembered on relogin. */
    fun setLastLocation(location: StoredLocation) {
        settings.putDouble(KEY_LOC_LAT, location.latitude)
        settings.putDouble(KEY_LOC_LNG, location.longitude)
        settings.putString(KEY_LOC_CITY, location.city.orEmpty())
        settings.putString(KEY_LOC_STATE, location.state.orEmpty())
        settings.putString(KEY_LOC_COUNTRY, location.countryCode.orEmpty())
        settings.putString(KEY_LOC_ADDRESS, location.addressLine.orEmpty())
        settings.putLong(KEY_LOC_UPDATED, currentTimeMillis())
    }

    fun lastLocation(): StoredLocation? {
        val lat = settings.getDoubleOrNull(KEY_LOC_LAT) ?: return null
        val lng = settings.getDoubleOrNull(KEY_LOC_LNG) ?: return null
        return StoredLocation(
            latitude = lat,
            longitude = lng,
            city = settings.getStringOrNull(KEY_LOC_CITY)?.takeIf { it.isNotBlank() },
            state = settings.getStringOrNull(KEY_LOC_STATE)?.takeIf { it.isNotBlank() },
            countryCode = settings.getStringOrNull(KEY_LOC_COUNTRY)?.takeIf { it.isNotBlank() },
            addressLine = settings.getStringOrNull(KEY_LOC_ADDRESS)?.takeIf { it.isNotBlank() },
            updatedAt = settings.getLongOrNull(KEY_LOC_UPDATED)
        )
    }

    /** Per-region timestamp of the last successful remote catalog sync. */
    fun catalogSyncedAt(region: String): Long? = settings.getLongOrNull(KEY_CATALOG_SYNC_PREFIX + region)

    fun setCatalogSyncedAt(region: String, timestamp: Long) {
        settings.putLong(KEY_CATALOG_SYNC_PREFIX + region, timestamp)
    }

    companion object {
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_PENDING_COUNTRY = "pending_country_code"
        private const val KEY_PENDING_LANGUAGE = "pending_language_code"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_PHONE_COUNTRY = "phone_country_code"
        private const val KEY_LOC_LAT = "last_location_lat"
        private const val KEY_LOC_LNG = "last_location_lng"
        private const val KEY_LOC_CITY = "last_location_city"
        private const val KEY_LOC_STATE = "last_location_state"
        private const val KEY_LOC_COUNTRY = "last_location_country"
        private const val KEY_LOC_ADDRESS = "last_location_address"
        private const val KEY_LOC_UPDATED = "last_location_updated_at"
        private const val KEY_CATALOG_SYNC_PREFIX = "catalog_synced_at_"
        // Used only if a screen renders before login (e.g. previews); real flows always set a user first.
        const val FALLBACK_USER_ID = "guest"
    }
}

/** Location snapshot persisted by [SessionManager.setLastLocation]. */
data class StoredLocation(
    val latitude: Double,
    val longitude: Double,
    val city: String?,
    val state: String?,
    val countryCode: String?,
    val addressLine: String?,
    val updatedAt: Long? = null
)
