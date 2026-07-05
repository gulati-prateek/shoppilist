package com.shoppilist.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the logged-in user's identity. There's no real auth backend in this project, so
 * Login/Register just upsert a UserEntity and record its id here — everything that needs
 * "who am I" (assignment, attribution, history, invites) reads through this instead of the
 * hardcoded "current_user_id" placeholders the scaffold used to have.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("shoppilist_session", Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow(prefs.getString(KEY_USER_ID, null))
    val currentUserId: StateFlow<String?> = _currentUserId

    fun requireUserId(): String = _currentUserId.value ?: FALLBACK_USER_ID

    fun setCurrentUser(userId: String) {
        prefs.edit { putString(KEY_USER_ID, userId) }
        _currentUserId.value = userId
    }

    fun clear() {
        prefs.edit { remove(KEY_USER_ID) }
        _currentUserId.value = null
    }

    /** Country/language picked during onboarding, before a user account exists yet — applied at login/register. */
    fun setPendingLocale(countryCode: String, languageCode: String) {
        prefs.edit {
            putString(KEY_PENDING_COUNTRY, countryCode)
            putString(KEY_PENDING_LANGUAGE, languageCode)
        }
    }

    fun consumePendingLocale(): Pair<String, String>? {
        val country = prefs.getString(KEY_PENDING_COUNTRY, null)
        val language = prefs.getString(KEY_PENDING_LANGUAGE, null)
        if (country == null || language == null) return null
        prefs.edit {
            remove(KEY_PENDING_COUNTRY)
            remove(KEY_PENDING_LANGUAGE)
        }
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
