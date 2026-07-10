package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.auth.AuthService
import com.shoppilist.shared.backend.ProfileBackend
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.local.UserEntity
import com.shoppilist.shared.data.session.SessionManager
import com.shoppilist.shared.data.session.StoredLocation
import com.shoppilist.shared.domain.UserAccountSync
import com.shoppilist.shared.backend.RemoteProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the app should land after the splash screen. */
enum class StartDestination { HOME, PROFILE_SETUP, LOGIN, ONBOARDING }

/**
 * Session restore + the launch-time verification gate: a signed-in *verified* account skips
 * login (straight to Home, or profile setup if the profile form was never completed); anything
 * less lands on Login/Onboarding. An unverified session can therefore never reach the dashboard,
 * on any path — Login's own flow re-applies the same gate via AuthUiState.verifiedUser.
 */
class SplashViewModel(
    private val authService: AuthService,
    private val accountSync: UserAccountSync,
    private val sessionManager: SessionManager
) : ViewModel() {

    suspend fun resolveStartDestination(): StartDestination {
        val user = try {
            authService.currentUser(refresh = true)
        } catch (_: Exception) {
            null
        }
        return when {
            user != null && user.isVerified -> {
                val local = accountSync.ensureLocalUser(user)
                if (local.firstName.isNullOrBlank()) StartDestination.PROFILE_SETUP else StartDestination.HOME
            }
            sessionManager.onboardingDone -> StartDestination.LOGIN
            else -> StartDestination.ONBOARDING
        }
    }
}

data class ProfileSetupUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val initialFirstName: String = "",
    val initialLastName: String = "",
    val initialEmail: String = "",
    /** True when the account itself carries an email (email sign-up) — shown read-only. */
    val emailLocked: Boolean = false,
    val initialAddress: String = "",
    val initialCity: String = "",
    val initialState: String = "",
    val initialPincode: String = ""
)

/**
 * First-run registration form (first name required; last name, email, address optional).
 * Prefills from whatever is already known — e.g. an email registrant's "full name" is split
 * into first/last — so for most users this is a confirm-and-continue screen.
 */
class ProfileViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val profileBackend: ProfileBackend,
    private val authService: AuthService
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSetupUiState())
    val state: StateFlow<ProfileSetupUiState> = _state

    fun load() {
        viewModelScope.launch {
            val user = userDao.getUserOnce(sessionManager.requireUserId())
            val accountEmail = authService.currentUser()?.email
            // A fullName that's just the mirrored email/phone isn't a real name — don't prefill it.
            val nameGuess = user?.fullName
                ?.takeIf { it.isNotBlank() && !it.contains("@") && !it.startsWith("+") && it != "You" }
            _state.update {
                it.copy(
                    loading = false,
                    initialFirstName = user?.firstName ?: nameGuess?.substringBefore(" ").orEmpty(),
                    initialLastName = user?.lastName
                        ?: nameGuess?.substringAfter(" ", "")?.trim().orEmpty(),
                    initialEmail = accountEmail ?: user?.email.orEmpty(),
                    emailLocked = !accountEmail.isNullOrBlank(),
                    initialAddress = user?.address.orEmpty(),
                    initialCity = user?.city.orEmpty(),
                    initialState = user?.state.orEmpty(),
                    initialPincode = user?.pincode.orEmpty()
                )
            }
        }
    }

    fun save(
        firstName: String,
        lastName: String,
        email: String,
        address: String,
        city: String = "",
        state: String = "",
        pincode: String = "",
        location: StoredLocation? = null
    ) {
        val first = firstName.trim()
        if (first.isEmpty()) {
            _state.update { it.copy(error = "First name is required") }
            return
        }
        val last = lastName.trim()
        val formEmail = email.trim()
        if (formEmail.isNotEmpty() && !formEmail.contains("@")) {
            _state.update { it.copy(error = "Enter a valid email address (or leave it empty)") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            location?.let { sessionManager.setLastLocation(it) }
            val userId = sessionManager.requireUserId()
            val existing = userDao.getUserOnce(userId)
            val updated = (existing ?: UserEntity(
                userId = userId,
                fullName = "",
                phone = null,
                email = null,
                country = null,
                state = null,
                city = null,
                pincode = null,
                profileImageUrl = null
            )).let { base ->
                base.copy(
                    firstName = first,
                    lastName = last.ifBlank { null },
                    fullName = "$first $last".trim(),
                    // The account's own (verifiable) email always wins over the optional form field.
                    email = if (_state.value.emailLocked) base.email else formEmail.ifBlank { base.email },
                    address = address.trim().ifBlank { base.address },
                    // Granular fields entered directly, else GPS reverse-geocode fallback, else keep.
                    city = city.trim().ifBlank { location?.city ?: base.city },
                    state = state.trim().ifBlank { location?.state ?: base.state },
                    pincode = pincode.trim().ifBlank { base.pincode },
                    countryCode = location?.countryCode ?: base.countryCode
                )
            }
            userDao.upsert(updated)
            profileBackend.saveProfile(
                RemoteProfile(
                    uid = userId,
                    firstName = updated.firstName,
                    lastName = updated.lastName,
                    email = updated.email,
                    phone = updated.phone,
                    address = updated.address,
                    city = updated.city,
                    state = updated.state,
                    countryCode = updated.countryCode,
                    pincode = updated.pincode,
                    languageCode = updated.languageCode,
                    lastLocation = location
                )
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
