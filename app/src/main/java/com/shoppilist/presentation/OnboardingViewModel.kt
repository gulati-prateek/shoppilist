package com.shoppilist.presentation

import androidx.lifecycle.ViewModel
import com.shoppilist.data.session.SessionManager

class OnboardingViewModel(private val sessionManager: SessionManager) : ViewModel() {
    fun selectLocale(countryCode: String, languageCode: String) {
        sessionManager.setPendingLocale(countryCode, languageCode)
    }
}
