package com.shoppilist.shared.auth

/** Placeholder until a real iOS auth integration exists (Phase 7) — same pattern as the iOS
 *  VoiceInputController stub. Keeps Koin's graph satisfiable so the iOS app still launches. */
class StubAuthService : AuthService {
    private val notAvailable = "Sign-in isn't available on iOS yet"

    override val isAvailable: Boolean = false

    override suspend fun currentUser(refresh: Boolean): AuthUser? = null

    override suspend fun registerWithEmail(email: String, password: String): Result<AuthUser> =
        Result.failure(IllegalStateException(notAvailable))

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> =
        Result.failure(IllegalStateException(notAvailable))

    override suspend fun sendEmailVerification(): Result<Unit> =
        Result.failure(IllegalStateException(notAvailable))

    override fun startPhoneVerification(
        phoneNumber: String,
        uiHost: Any?,
        onCodeSent: () -> Unit,
        onVerified: (AuthUser) -> Unit,
        onError: (String) -> Unit
    ) = onError(notAvailable)

    override fun submitOtp(code: String, onVerified: (AuthUser) -> Unit, onError: (String) -> Unit) =
        onError(notAvailable)

    override fun signOut() {}
}
