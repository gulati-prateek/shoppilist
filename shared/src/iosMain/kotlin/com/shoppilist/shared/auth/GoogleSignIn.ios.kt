package com.shoppilist.shared.auth

import androidx.compose.runtime.Composable

/** iOS placeholder until GoogleSignIn-iOS is wired (Phase 3 iOS). Reports unavailable. */
@Composable
actual fun rememberGoogleSignIn(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
): GoogleSignInController = object : GoogleSignInController {
    override val isAvailable: Boolean = false
    override fun launch() = onError("Google sign-in isn't available on iOS yet")
}
