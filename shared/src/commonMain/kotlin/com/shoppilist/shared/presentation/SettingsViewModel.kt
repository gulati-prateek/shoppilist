package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.auth.AuthService
import com.shoppilist.shared.auth.AuthUser
import com.shoppilist.shared.backend.AdminBackend
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.local.UserEntity
import com.shoppilist.shared.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val authService: AuthService,
    private val adminBackend: AdminBackend
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

    fun setHideSponsoredLinks(hide: Boolean) = updateUser { it.copy(hideSponsoredLinks = hide) }

    fun setGroceryCardDismissed(dismissed: Boolean) = updateUser { it.copy(groceryCardDismissed = dismissed) }

    fun setLocale(countryCode: String, languageCode: String) =
        updateUser { it.copy(countryCode = countryCode, languageCode = languageCode) }

    /** Item 6: sign out. Clears the Firebase session and the local current-user pointer, then the
     *  screen navigates back to Login. The local data + remembered location survive (see
     *  SessionManager.clear), so a re-login on this device restores instantly. */
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut

    fun logout() {
        viewModelScope.launch {
            runCatching { authService.signOut() }
            sessionManager.clear()
            _loggedOut.value = true
        }
    }

    private fun updateUser(transform: (UserEntity) -> UserEntity) {
        val current = _user.value ?: return
        viewModelScope.launch { userDao.upsert(transform(current)) }
    }
}
