package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import com.shoppilist.shared.data.session.SessionManager

class OnboardingViewModel(private val sessionManager: SessionManager) : ViewModel() {
    fun selectLocale(countryCode: String, languageCode: String) {
        sessionManager.setPendingLocale(countryCode, languageCode)
        // Relaunches now skip the intro/locale flow and go straight to Login (Splash checks this).
        sessionManager.onboardingDone = true
    }
}
