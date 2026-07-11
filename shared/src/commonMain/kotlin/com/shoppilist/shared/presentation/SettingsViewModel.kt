package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.auth.AuthService
import com.shoppilist.shared.auth.AuthUser
import com.shoppilist.shared.backend.AdminBackend
import com.shoppilist.shared.backend.ProfileBackend
import com.shoppilist.shared.backend.RemoteProfile
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.local.UserEntity
import com.shoppilist.shared.data.session.SessionManager
import com.shoppilist.shared.sync.CollaborationSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val authService: AuthService,
    private val adminBackend: AdminBackend,
    private val profileBackend: ProfileBackend,
    private val collaborationSync: CollaborationSyncManager
) : ViewModel() {

    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user

    /** Live verification status from the auth backend (issue 8's profile badges). */
    private val _authUser = MutableStateFlow<AuthUser?>(null)
    val authUser: StateFlow<AuthUser?> = _authUser

    /** Whether the signed-in account has an `admins/{uid}` marker — gates the Admin card. */
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    fun load() {
        viewModelScope.launch { _authUser.value = authService.currentUser(refresh = true) }
        viewModelScope.launch { _isAdmin.value = adminBackend.isAdmin(sessionManager.requireUserId()) }
        viewModelScope.launch {
            userDao.getUserFlow(sessionManager.requireUserId()).collect { _user.value = it }
        }
    }

    // --- Profile "Add email / Add phone" + verification (account card actions) ---

    private val _accountInfo = MutableStateFlow<String?>(null)
    val accountInfo: StateFlow<String?> = _accountInfo

    private val _accountError = MutableStateFlow<String?>(null)
    val accountError: StateFlow<String?> = _accountError

    /** True once the add-phone SMS was sent — the dialog switches to its OTP entry step. */
    private val _addPhoneOtpSent = MutableStateFlow(false)
    val addPhoneOtpSent: StateFlow<Boolean> = _addPhoneOtpSent

    /** Adds email+password sign-in to the account, then sends the verification link. */
    fun addEmail(email: String, password: String) {
        val trimmed = email.trim()
        if (!trimmed.contains("@")) { _accountError.value = "Enter a valid email address"; return }
        if (password.length < 6) { _accountError.value = "Password must be at least 6 characters"; return }
        viewModelScope.launch {
            _accountError.value = null
            authService.linkEmail(trimmed, password)
                .onSuccess { linked ->
                    val sent = authService.sendEmailVerification()
                    _authUser.value = linked
                    updateUser { it.copy(email = trimmed) }
                    _accountInfo.value =
                        if (sent.isSuccess) "Verification link sent to $trimmed — tap it, then Refresh"
                        else "Email added, but the verification link couldn't be sent — tap Verify to retry"
                }
                .onFailure { _accountError.value = it.message ?: "Couldn't add the email" }
        }
    }

    /** Re-sends the verification link for an existing-but-unverified email. */
    fun sendEmailVerification() {
        viewModelScope.launch {
            _accountError.value = null
            authService.sendEmailVerification()
                .onSuccess { _accountInfo.value = "Verification link sent — tap it, then Refresh" }
                .onFailure { _accountError.value = it.message ?: "Couldn't send the link" }
        }
    }

    /** Re-reads the account from the server (e.g. after the user clicked the email link). */
    fun refreshAccountStatus() {
        viewModelScope.launch {
            _authUser.value = authService.currentUser(refresh = true)
            _accountInfo.value = null
        }
    }

    /** Starts the OTP flow that links [phoneNumber] to the signed-in account. */
    fun startAddPhone(phoneNumber: String, uiHost: Any?) {
        _accountError.value = null
        authService.startPhoneLink(
            phoneNumber = phoneNumber,
            uiHost = uiHost,
            onCodeSent = { _addPhoneOtpSent.value = true },
            onVerified = ::onPhoneLinked,
            onError = { _accountError.value = it }
        )
    }

    fun submitAddPhoneOtp(code: String) {
        authService.submitLinkOtp(code, onVerified = ::onPhoneLinked, onError = { _accountError.value = it })
    }

    private fun onPhoneLinked(linked: AuthUser) {
        _authUser.value = linked
        _addPhoneOtpSent.value = false
        linked.phoneNumber?.let { phone -> updateUser { it.copy(phone = phone) } }
        _accountInfo.value = "Phone number added and verified"
    }

    /** Dialog dismissed — clear the in-flight add-phone state so a reopen starts fresh. */
    fun resetAddPhone() {
        _addPhoneOtpSent.value = false
        _accountError.value = null
    }

    fun setHideSponsoredLinks(hide: Boolean) = updateUser { it.copy(hideSponsoredLinks = hide) }

    fun setGroceryCardDismissed(dismissed: Boolean) = updateUser { it.copy(groceryCardDismissed = dismissed) }

    fun setLocale(countryCode: String, languageCode: String) =
        updateUser { it.copy(countryCode = countryCode, languageCode = languageCode) }

    private val _profileSaved = MutableStateFlow(false)
    val profileSaved: StateFlow<Boolean> = _profileSaved

    /** Item 3 / P2: save edited name + address + country from the Profile screen. */
    fun saveProfile(
        fullName: String,
        country: String?,
        state: String,
        city: String,
        pincode: String,
        address: String
    ) {
        val current = _user.value ?: return
        viewModelScope.launch {
            val parts = fullName.trim().split(" ", limit = 2)
            val updated = current.copy(
                fullName = fullName.trim().ifBlank { current.fullName },
                firstName = parts.getOrNull(0)?.ifBlank { null } ?: current.firstName,
                lastName = parts.getOrNull(1)?.trim()?.ifBlank { null },
                countryCode = country ?: current.countryCode,
                state = state.trim().ifBlank { current.state },
                city = city.trim().ifBlank { current.city },
                pincode = pincode.trim().ifBlank { current.pincode },
                address = address.trim().ifBlank { current.address }
            )
            userDao.upsert(updated)
            profileBackend.saveProfile(
                RemoteProfile(
                    uid = updated.userId,
                    firstName = updated.firstName,
                    lastName = updated.lastName,
                    email = updated.email,
                    phone = updated.phone,
                    address = updated.address,
                    city = updated.city,
                    state = updated.state,
                    countryCode = updated.countryCode,
                    pincode = updated.pincode,
                    languageCode = updated.languageCode
                )
            )
            _profileSaved.value = true
        }
    }

    fun ackProfileSaved() { _profileSaved.value = false }

    /** Item 6: sign out. Clears the Firebase session and the local current-user pointer, then the
     *  screen navigates back to Login. The local data + remembered location survive (see
     *  SessionManager.clear), so a re-login on this device restores instantly. */
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut

    fun logout() {
        viewModelScope.launch {
            // B4: tear down the Firestore listeners BEFORE clearing the session, so nothing keeps
            // mirroring this account's lists into whoever signs in next on this device.
            collaborationSync.stop()
            runCatching { authService.signOut() }
            sessionManager.clear()
            _loggedOut.value = true
        }
    }

    /** C3: change the password of an email+password account (re-authenticates with the current
     *  password first). */
    fun changePassword(currentPassword: String, newPassword: String) {
        if (newPassword.length < 6) {
            _accountError.value = "New password must be at least 6 characters"
            return
        }
        viewModelScope.launch {
            _accountError.value = null
            authService.changePassword(currentPassword, newPassword)
                .onSuccess { _accountInfo.value = "Password changed" }
                .onFailure { _accountError.value = it.message ?: "Couldn't change the password" }
        }
    }

    /** C1 (Play account-deletion policy): permanently deletes the account and its data.
     *
     * Order matters: the Firestore wipe must run while still authenticated (rules gate the
     * deletes), so remote data goes first, then the auth account, then this device's local data.
     * If the auth deletion fails with "recent login required", the user re-logs-in and retries —
     * the remote wipe is then a no-op and the deletion completes. */
    private val _deletingAccount = MutableStateFlow(false)
    val deletingAccount: StateFlow<Boolean> = _deletingAccount

    fun deleteAccount() {
        viewModelScope.launch {
            _accountError.value = null
            _deletingAccount.value = true
            val uid = sessionManager.requireUserId()
            runCatching { collaborationSync.wipeRemoteOnAccountDeletion(uid) }
            runCatching { profileBackend.deleteProfile(uid) }
            authService.deleteAccount()
                .onSuccess {
                    runCatching { collaborationSync.wipeLocalOnAccountDeletion(uid) }
                    sessionManager.clear()
                    _deletingAccount.value = false
                    _loggedOut.value = true
                }
                .onFailure {
                    _deletingAccount.value = false
                    _accountError.value = it.message ?: "Couldn't delete the account"
                }
        }
    }

    private fun updateUser(transform: (UserEntity) -> UserEntity) {
        val current = _user.value ?: return
        viewModelScope.launch { userDao.upsert(transform(current)) }
    }
}
