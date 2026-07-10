package com.shoppilist.shared.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

/**
 * Android Google sign-in via Credential Manager. The Web (server) client id is read at runtime from
 * the `default_web_client_id` string resource that the google-services plugin generates once a
 * Google OAuth client exists in the Firebase project — so this stays compile-safe and simply reports
 * unavailable until the Firebase console's Google provider is enabled.
 */
@Composable
actual fun rememberGoogleSignIn(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
): GoogleSignInController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webClientId = remember {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id != 0) context.getString(id) else null
    }
    return remember(webClientId) {
        object : GoogleSignInController {
            override val isAvailable: Boolean = webClientId != null

            override fun launch() {
                val clientId = webClientId ?: run {
                    onError("Google sign-in isn't set up yet")
                    return
                }
                val option = GetGoogleIdOption.Builder()
                    .setServerClientId(clientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                scope.launch {
                    try {
                        val result = CredentialManager.create(context).getCredential(context, request)
                        val cred = result.credential
                        if (cred is CustomCredential &&
                            cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            onIdToken(GoogleIdTokenCredential.createFrom(cred.data).idToken)
                        } else {
                            onError("Unexpected sign-in response")
                        }
                    } catch (e: Exception) {
                        onError(e.message ?: "Google sign-in failed")
                    }
                }
            }
        }
    }
}
