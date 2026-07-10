package com.shoppilist.shared.auth

/** Snapshot of the signed-in identity as the auth backend sees it. */
data class AuthUser(
    val uid: String,
    val email: String?,
    val emailVerified: Boolean,
    val phoneNumber: String?
) {
    /** Gate for reaching the dashboard (§issues 8/9): a verified email OR an OTP-verified
     *  phone number (phone sign-in is verified by definition — you can't complete it without
     *  receiving the SMS). */
    val isVerified: Boolean get() = emailVerified || !phoneNumber.isNullOrBlank()
}

/**
 * Abstracts identity verification (Firebase Auth has no Kotlin/Native artifact). The Android
 * implementation lives in `:app` (`FirebaseAuthService`, where the Firebase dependency is);
 * iOS registers a stub that reports unavailable until Phase 7 brings a real iOS integration.
 *
 * Phone verification is callback-based because Firebase's OTP flow is: request → SMS arrives
 * out-of-band → user types code (or Android auto-retrieves it, skipping straight to verified).
 */
interface AuthService {
    val isAvailable: Boolean

    /** Currently signed-in user, or null. `refresh = true` re-reads from the server so a
     *  just-clicked email-verification link is reflected. */
    suspend fun currentUser(refresh: Boolean = false): AuthUser?

    /** Creates the account only — callers follow up with [sendEmailVerification] so that a
     *  send failure is visible to the user rather than silently swallowed. */
    suspend fun registerWithEmail(email: String, password: String): Result<AuthUser>

    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>

    suspend fun sendEmailVerification(): Result<Unit>

    /** Sends a password-reset email to [email] (item 5). No-op success is not assumed — errors surface. */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /** Exchanges a Google ID token (obtained by the platform sign-in UI) for a Firebase session. */
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    /**
     * Starts phone-number verification. [uiHost] is the platform UI handle the backend needs
     * (the current Activity on Android, for the reCAPTCHA/Play-Integrity fallback) — obtain it
     * with [rememberAuthUiHost]. Exactly one of [onVerified] (auto-retrieval) or [onCodeSent]
     * (manual entry needed → call [submitOtp]) fires on success.
     */
    fun startPhoneVerification(
        phoneNumber: String,
        uiHost: Any?,
        onCodeSent: () -> Unit,
        onVerified: (AuthUser) -> Unit,
        onError: (String) -> Unit
    )

    /** Completes phone sign-in with the SMS code from the latest [startPhoneVerification]. */
    fun submitOtp(code: String, onVerified: (AuthUser) -> Unit, onError: (String) -> Unit)

    fun signOut()
}
