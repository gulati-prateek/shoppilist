package com.shoppilist.shared.auth

import androidx.compose.runtime.Composable

/** Platform handle for launching the Google sign-in UI (Credential Manager on Android). */
interface GoogleSignInController {
    /** False when Google sign-in isn't configured/available on this platform yet. */
    val isAvailable: Boolean
    /** Launches the account picker; the token/error is delivered to the callbacks supplied to
     *  [rememberGoogleSignIn]. */
    fun launch()
}

/**
 * Remembers a controller that launches the platform's Google sign-in and hands back a Google ID
 * token (to be exchanged for a Firebase credential via [AuthService.signInWithGoogle]).
 */
@Composable
expect fun rememberGoogleSignIn(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
): GoogleSignInController
