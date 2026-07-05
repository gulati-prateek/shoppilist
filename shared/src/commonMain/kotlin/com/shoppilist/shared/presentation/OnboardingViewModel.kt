package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import com.shoppilist.shared.data.session.SessionManager

class OnboardingViewModel(private val sessionManager: SessionManager) : ViewModel() {
    fun selectLocale(countryCode: String, languageCode: String) {
        sessionManager.setPendingLocale(countryCode, languageCode)
    }
}
