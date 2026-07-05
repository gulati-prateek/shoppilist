package com.shoppilist.shared.data.session

import com.russhwolf.settings.Settings
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

    companion object {
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_PENDING_COUNTRY = "pending_country_code"
        private const val KEY_PENDING_LANGUAGE = "pending_language_code"
        // Used only if a screen renders before login (e.g. previews); real flows always set a user first.
        const val FALLBACK_USER_ID = "guest"
    }
}
