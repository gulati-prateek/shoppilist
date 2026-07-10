package com.shoppilist.shared.domain

import com.shoppilist.shared.auth.AuthUser
import com.shoppilist.shared.backend.ProfileBackend
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.local.UserEntity
import com.shoppilist.shared.data.session.SessionManager

/**
 * Mirrors the authenticated account into the local Room user row (keyed by the auth UID) and
 * records the session. Shared by AuthViewModel (login/register) and SplashViewModel (session
 * restore) so both paths apply identical rules.
 *
 * On a fresh install the cloud profile mirror is consulted first, so a returning user keeps
 * their name/address/last-location instead of being treated as brand new.
 */
class UserAccountSync(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val profileBackend: ProfileBackend
) {

    /** Returns the up-to-date local row; a null/blank [UserEntity.firstName] on it means the
     *  profile-setup form still needs to run. */
    suspend fun ensureLocalUser(user: AuthUser, fullName: String? = null): UserEntity {
        val pendingLocale = sessionManager.consumePendingLocale()
        val existing = userDao.getUserOnce(user.uid) ?: restoreFromCloud(user)
        val base = existing?.copy(
            fullName = fullName?.takeIf { it.isNotBlank() } ?: existing.fullName,
            email = user.email ?: existing.email,
            phone = user.phoneNumber ?: existing.phone
        ) ?: UserEntity(
            userId = user.uid,
            fullName = fullName?.takeIf { it.isNotBlank() } ?: user.email ?: user.phoneNumber ?: "You",
            phone = user.phoneNumber,
            email = user.email,
            country = null,
            state = null,
            city = null,
            pincode = null,
            profileImageUrl = null
        )
        val withLocale = if (pendingLocale != null) {
            base.copy(countryCode = pendingLocale.first, languageCode = pendingLocale.second)
        } else base
        userDao.upsert(withLocale)
        sessionManager.setCurrentUser(user.uid)
        return withLocale
    }

    private suspend fun restoreFromCloud(user: AuthUser): UserEntity? {
        val remote = profileBackend.fetchProfile(user.uid) ?: return null
        remote.lastLocation?.let { sessionManager.setLastLocation(it) }
        return UserEntity(
            userId = user.uid,
            fullName = listOfNotNull(remote.firstName, remote.lastName).joinToString(" ")
                .ifBlank { user.email ?: user.phoneNumber ?: "You" },
            phone = user.phoneNumber ?: remote.phone,
            email = user.email ?: remote.email,
            country = null,
            state = remote.state,
            city = remote.city,
            pincode = remote.pincode,
            profileImageUrl = null,
            firstName = remote.firstName,
            lastName = remote.lastName,
            address = remote.address,
            countryCode = remote.countryCode,
            languageCode = remote.languageCode
        )
    }
}
