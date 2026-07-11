package com.shoppilist.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.shoppilist.shared.auth.AuthService
import com.shoppilist.shared.auth.AuthUser
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FirebaseAuthService : AuthService {

    private val auth = FirebaseAuth.getInstance()

    // Phone verification is a two-step, out-of-band flow: the id from onCodeSent must survive
    // until the user types the SMS code and submitOtp()/submitLinkOtp() runs. pendingIsLink
    // remembers whether that pending code signs in or links to the current account.
    private var pendingVerificationId: String? = null
    private var pendingIsLink: Boolean = false

    override val isAvailable: Boolean = true

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        emailVerified = isEmailVerified,
        phoneNumber = phoneNumber?.takeIf { it.isNotBlank() }
    )

    override suspend fun currentUser(refresh: Boolean): AuthUser? {
        val user = auth.currentUser ?: return null
        if (refresh) {
            // Offline reload failures shouldn't hide an existing session — fall back to cached state.
            try { user.reload().await() } catch (_: Exception) {}
        }
        return (auth.currentUser ?: return null).toAuthUser()
    }

    override suspend fun registerWithEmail(email: String, password: String): Result<AuthUser> =
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw IllegalStateException("Registration returned no user")
            // The verification email is sent by the caller (AuthViewModel) via
            // sendEmailVerification() so a send failure reaches the UI instead of the app
            // claiming "email sent" when Firebase actually rejected it.
            Result.success(user.toAuthUser())
        } catch (e: Exception) {
            Result.failure(friendly(e))
        }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> =
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw IllegalStateException("Sign-in returned no user")
            Result.success(user.toAuthUser())
        } catch (e: Exception) {
            Result.failure(friendly(e))
        }

    override suspend fun sendEmailVerification(): Result<Unit> =
        try {
            val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(friendly(e))
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(friendly(e))
        }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> =
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = auth.signInWithCredential(credential).await().user
                ?: throw IllegalStateException("Google sign-in returned no user")
            Result.success(user.toAuthUser())
        } catch (e: Exception) {
            Result.failure(friendly(e))
        }

    /** Firebase's raw messages are developer-speak; translate the recoverable ones. */
    private fun friendly(e: Exception): Exception = when (e) {
        is FirebaseTooManyRequestsException ->
            Exception("Too many attempts from this device — wait a while and try again", e)
        is FirebaseNetworkException ->
            Exception("No internet connection — check your network and try again", e)
        is FirebaseAuthUserCollisionException ->
            Exception("That email is already registered — try logging in instead", e)
        else -> e
    }

    override fun startPhoneVerification(
        phoneNumber: String,
        uiHost: Any?,
        onCodeSent: () -> Unit,
        onVerified: (AuthUser) -> Unit,
        onError: (String) -> Unit
    ) = startPhoneFlow(phoneNumber, uiHost, link = false, onCodeSent, onVerified, onError)

    override fun startPhoneLink(
        phoneNumber: String,
        uiHost: Any?,
        onCodeSent: () -> Unit,
        onVerified: (AuthUser) -> Unit,
        onError: (String) -> Unit
    ) = startPhoneFlow(phoneNumber, uiHost, link = true, onCodeSent, onVerified, onError)

    /** Shared OTP plumbing; [link] decides whether the verified credential signs the user in or
     *  is attached to the already-signed-in account (Profile "Add phone"). */
    private fun startPhoneFlow(
        phoneNumber: String,
        uiHost: Any?,
        link: Boolean,
        onCodeSent: () -> Unit,
        onVerified: (AuthUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val activity = uiHost as? Activity
        if (activity == null) {
            onError("Phone verification isn't available right now")
            return
        }
        if (link && auth.currentUser == null) {
            onError("Sign in first to add a phone number")
            return
        }
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Android auto-retrieved the SMS — no manual code entry needed.
                completePhoneCredential(credential, link, onVerified, onError)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onError(friendly(e).message ?: "Phone verification failed")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                pendingVerificationId = verificationId
                pendingIsLink = link
                onCodeSent()
            }
        }
        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        )
    }

    override fun submitOtp(code: String, onVerified: (AuthUser) -> Unit, onError: (String) -> Unit) =
        submitPendingOtp(code, expectLink = false, onVerified, onError)

    override fun submitLinkOtp(code: String, onVerified: (AuthUser) -> Unit, onError: (String) -> Unit) =
        submitPendingOtp(code, expectLink = true, onVerified, onError)

    private fun submitPendingOtp(
        code: String,
        expectLink: Boolean,
        onVerified: (AuthUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val verificationId = pendingVerificationId
        if (verificationId == null || pendingIsLink != expectLink) {
            onError("Request a code first")
            return
        }
        completePhoneCredential(
            PhoneAuthProvider.getCredential(verificationId, code), expectLink, onVerified, onError
        )
    }

    private fun completePhoneCredential(
        credential: PhoneAuthCredential,
        link: Boolean,
        onVerified: (AuthUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val task = if (link) {
            val current = auth.currentUser
            if (current == null) {
                onError("Sign in first to add a phone number")
                return
            }
            current.linkWithCredential(credential)
        } else {
            auth.signInWithCredential(credential)
        }
        task
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) onVerified(user.toAuthUser()) else onError("Sign-in failed")
            }
            .addOnFailureListener { e ->
                onError(
                    if (e is FirebaseAuthUserCollisionException)
                        "That phone number is already used by another account"
                    else e.message ?: "Invalid code"
                )
            }
    }

    override suspend fun linkEmail(email: String, password: String): Result<AuthUser> =
        try {
            val current = auth.currentUser ?: throw IllegalStateException("Not signed in")
            val credential = EmailAuthProvider.getCredential(email.trim(), password)
            val user = current.linkWithCredential(credential).await().user
                ?: throw IllegalStateException("Linking returned no user")
            Result.success(user.toAuthUser())
        } catch (e: Exception) {
            Result.failure(
                if (e is FirebaseAuthUserCollisionException)
                    Exception("That email is already used by another account", e)
                else friendly(e)
            )
        }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        try {
            val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
            val email = user.email
                ?: throw IllegalStateException("This account has no email password to change")
            // Firebase requires a recent login for credential changes; re-authenticating with the
            // current password satisfies that and doubles as verification the user knows it.
            user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                if (e is FirebaseAuthInvalidCredentialsException)
                    Exception("Current password is incorrect", e)
                else friendly(e)
            )
        }

    override suspend fun deleteAccount(): Result<Unit> =
        try {
            val user = auth.currentUser ?: throw IllegalStateException("Not signed in")
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                if (e is FirebaseAuthRecentLoginRequiredException)
                    Exception("For security, log out, sign in again, and then retry deleting your account", e)
                else friendly(e)
            )
        }

    override fun signOut() {
        auth.signOut()
    }
}
