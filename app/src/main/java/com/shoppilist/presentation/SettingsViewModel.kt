package com.shoppilist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.local.UserEntity
import com.shoppilist.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user

    fun load() {
        viewModelScope.launch {
            userDao.getUserFlow(sessionManager.requireUserId()).collect { _user.value = it }
        }
    }

    fun setHideSponsoredLinks(hide: Boolean) = updateUser { it.copy(hideSponsoredLinks = hide) }

    fun setGroceryCardDismissed(dismissed: Boolean) = updateUser { it.copy(groceryCardDismissed = dismissed) }

    fun setLocale(countryCode: String, languageCode: String) =
        updateUser { it.copy(countryCode = countryCode, languageCode = languageCode) }

    private fun updateUser(transform: (UserEntity) -> UserEntity) {
        val current = _user.value ?: return
        viewModelScope.launch { userDao.upsert(transform(current)) }
    }
}
