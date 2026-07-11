@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.shoppilist.shared.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.koin.compose.viewmodel.koinViewModel
import com.shoppilist.shared.auth.rememberAuthUiHost
import com.shoppilist.shared.data.session.StoredLocation
import com.shoppilist.shared.domain.Country
import com.shoppilist.shared.location.rememberLocationController
import com.shoppilist.shared.presentation.AuthUiState
import com.shoppilist.shared.presentation.AuthViewModel
import com.shoppilist.shared.presentation.ProfileViewModel
import com.shoppilist.shared.presentation.SplashViewModel
import com.shoppilist.shared.presentation.StartDestination

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = koinViewModel(),
    onResolved: (StartDestination) -> Unit
) {
    LaunchedEffect(Unit) {
        // Resolve immediately — no artificial hold — so the app opens straight onto the login
        // form (or Home when a session is restored). The splash shares the auth backdrop, so the
        // brief moment it's visible reads as the login page loading, not a separate screen.
        onResolved(viewModel.resolveStartDestination())
    }
    // The themed backdrop is now global (AppNavigation wraps every screen in AuthBackground).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ShoppiList",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Shop anything, together — groceries, fashion, electronics & more.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun OnboardingScreen(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to ShoppiList", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Shop anything, together — groceries, fashion, electronics & more.")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNext) { Text("Get Started") }
    }
}

/** Plain label/value profile line (no verification badge). */
@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** One contact method (email/phone) with its verification badge (issue 8) and an optional
 *  trailing action ("Add" when missing, "Verify" when unverified). */
@Composable
private fun AccountContactRow(
    label: String,
    value: String?,
    verified: Boolean,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value ?: "Not added", style = MaterialTheme.typography.bodyMedium)
        }
        if (value != null) {
            Text(
                if (verified) "✓ Verified" else "Not verified",
                style = MaterialTheme.typography.labelMedium,
                color = if (verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        action?.invoke()
    }
}

/** Profile → "Add email": links email+password sign-in to the account, then a verification link
 *  is emailed (the account row keeps showing "Not verified" until it's clicked). */
@Composable
private fun AddEmailDialog(
    error: String?,
    onAdd: (email: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add email") },
        text = {
            Column {
                Text(
                    "This adds email & password sign-in to your account. We'll send a verification link.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("New password (min 6 characters)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(email, password) },
                enabled = email.contains("@") && password.length >= 6
            ) { Text("Add & send link") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Profile → "Add phone": OTP-verifies a number and links it to the signed-in account. */
@Composable
private fun AddPhoneDialog(
    defaultCountry: Country,
    otpSent: Boolean,
    error: String?,
    onSendCode: (fullNumber: String) -> Unit,
    onSubmitCode: (code: String) -> Unit,
    onDismiss: () -> Unit
) {
    var country by remember { mutableStateOf(defaultCountry) }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add phone number") },
        text = {
            Column {
                if (!otpSent) {
                    Text(
                        "We'll text a 6-digit code to verify this number.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PhoneNumberField(
                        country = country,
                        onCountryChange = { country = it },
                        phone = phone,
                        onPhoneChange = { phone = it }
                    )
                } else {
                    Text(
                        "Enter the code sent to ${country.dialCode} $phone",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                        label = { Text("6-digit code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (!otpSent) {
                TextButton(
                    onClick = { onSendCode(country.dialCode + phone) },
                    enabled = phone.length >= 6
                ) { Text("Send code") }
            } else {
                TextButton(onClick = { onSubmitCode(otp) }, enabled = otp.length == 6) { Text("Verify") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** C3: change the password of an email+password account (re-authenticates with the current one). */
@Composable
private fun ChangePasswordDialog(
    info: String?,
    error: String?,
    onChange: (current: String, new: String) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column {
                OutlinedTextField(
                    value = current, onValueChange = { current = it },
                    label = { Text("Current password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = new, onValueChange = { new = it },
                    label = { Text("New password (min 6 characters)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (confirm) {
                    info?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                    error?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirm = true; onChange(current, new) },
                enabled = current.isNotBlank() && new.length >= 6
            ) { Text("Change") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/** C1: permanent account deletion (Play account-deletion policy) with a strong warning. */
@Composable
private fun DeleteAccountDialog(
    deleting: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete account?") },
        text = {
            Column {
                Text(
                    "This permanently deletes your account, your profile, and every shared list you " +
                        "own (other members lose those lists too). Lists you joined stay with their owners. " +
                        "This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (deleting) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CircularProgressIndicator()
                }
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !deleting) {
                Text("Delete forever", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !deleting) { Text("Cancel") } }
    )
}

/**
 * C2: in-app Privacy Policy & Terms. PLACEHOLDER legal copy — replace with counsel-reviewed text
 * (or link a hosted policy URL) before submitting to the Play Store, which requires one.
 */
@Composable
internal fun LegalDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy Policy & Terms") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Privacy Policy", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "ShoppiList stores your account details (name, email/phone, address you provide), " +
                        "your shopping lists, and — when you share a list — the list contents and your " +
                        "display name with the people you invite. Data is stored on your device and in " +
                        "Google Firebase (Authentication & Cloud Firestore). We don't sell your data or " +
                        "show third-party ads. You can delete your account and its data anytime from " +
                        "Profile → Delete account.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Terms of Service", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "ShoppiList is provided as-is, without warranty. You're responsible for the content " +
                        "you add and share. Don't use the app for unlawful content or to spam others with " +
                        "invitations. We may suspend accounts that abuse the service.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

/** "Resend code" with a 30s cooldown so users don't hammer the SMS quota (and Firebase throttles). */
@Composable
private fun ResendCodeButton(enabled: Boolean, onResend: () -> Unit) {
    var resendKey by remember { mutableStateOf(0) }
    var secondsLeft by remember { mutableStateOf(30) }
    LaunchedEffect(resendKey) {
        secondsLeft = 30
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            secondsLeft--
        }
    }
    TextButton(
        onClick = { onResend(); resendKey++ },
        enabled = enabled && secondsLeft == 0
    ) {
        Text(if (secondsLeft > 0) "Resend code in ${secondsLeft}s" else "Resend code")
    }
}

/** Status/progress feedback shared by the Login and Register forms. */
@Composable
private fun AuthStatusMessages(state: AuthUiState) {
    if (state.loading) {
        Spacer(modifier = Modifier.height(12.dp))
        CircularProgressIndicator()
    }
    state.error?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    state.info?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

/** Shown after email registration/login until the verification link is clicked (issues 7 & 9). */
@Composable
private fun EmailVerificationPanel(state: AuthUiState, viewModel: AuthViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Verify your email", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We sent a verification link to ${state.registeredEmail ?: "your email"}. " +
                    "Open it, then come back here.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { viewModel.refreshVerificationStatus() }, enabled = !state.loading) {
                Text("I've verified — continue")
            }
            TextButton(onClick = { viewModel.resendVerificationEmail() }, enabled = !state.loading) {
                Text("Resend email")
            }
        }
    }
}

/** Email or Phone method switcher used by both auth screens. */
@Composable
private fun AuthMethodTabs(selected: Int, onSelect: (Int) -> Unit) {
    TabRow(selectedTabIndex = selected, modifier = Modifier.fillMaxWidth()) {
        Tab(selected = selected == 0, onClick = { onSelect(0) }, text = { Text("Email") })
        Tab(selected = selected == 1, onClick = { onSelect(1) }, text = { Text("Phone") })
    }
}

/** Phase 3: "Continue with Google" — hidden when Google sign-in isn't configured on this platform. */
@Composable
private fun GoogleSignInButton(onIdToken: (String) -> Unit, enabled: Boolean) {
    var error by remember { mutableStateOf<String?>(null) }
    val controller = com.shoppilist.shared.auth.rememberGoogleSignIn(
        onIdToken = onIdToken,
        onError = { error = it }
    )
    if (!controller.isAvailable) return
    OutlinedButton(
        onClick = { error = null; controller.launch() },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Continue with Google")
    }
    error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ForgotPasswordDialog(initialEmail: String, onSend: (String) -> Unit, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf(initialEmail) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset password") },
        text = {
            Column {
                Text(
                    "We'll email you a link to set a new password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Account email") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(email) }, enabled = email.contains("@")) { Text("Send link") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Phone-number entry with a tappable country-code selector. The code defaults to the country the
 * user picked at first use (persisted) / their device region, and changing it here remembers the
 * choice — so "+91" is no longer hard-coded. Filters to digits and caps at the E.164 length.
 */
@Composable
private fun PhoneNumberField(
    country: Country,
    onCountryChange: (Country) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { showPicker = true }, contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text("${country.flag} ${country.dialCode}")
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change country code")
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { onPhoneChange(it.filter(Char::isDigit).take(15)) },
            label = { Text("Phone number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
    if (showPicker) {
        Dialog(onDismissRequest = { showPicker = false }) {
            Card {
                CountryPickerList(
                    modifier = Modifier.padding(16.dp).heightIn(max = 480.dp),
                    onSelect = { picked -> onCountryChange(picked); showPicker = false }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onCreateAccount: () -> Unit = {},
    /** needsProfile = first-time account → the profile-setup form comes before Home. */
    onLoginSuccess: (needsProfile: Boolean) -> Unit,
    /** A3: hidden admin sign-in — reached by long-pressing the brand; routes to the admin panel. */
    onAdminLogin: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val uiHost = rememberAuthUiHost()
    var method by remember { mutableStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var showForgot by remember { mutableStateOf(false) }
    var adminMode by remember { mutableStateOf(false) }

    LaunchedEffect(state.verifiedUser) {
        if (state.verifiedUser != null) {
            if (adminMode) onAdminLogin() else onLoginSuccess(state.needsProfile)
        }
    }

    if (showForgot) {
        ForgotPasswordDialog(
            initialEmail = email,
            onSend = { viewModel.sendPasswordReset(it); showForgot = false },
            onDismiss = { showForgot = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Branded header — the login screen carries the ShoppiList splash branding (item 4).
        // Long-pressing the wordmark switches to the hidden admin sign-in (A3).
        Text(
            "ShoppiList",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { adminMode = true; method = 0 }
            )
        )
        Text(
            if (adminMode) "Admin sign-in" else "Shop anything, together",
            style = MaterialTheme.typography.bodyMedium,
            color = if (adminMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (state.awaitingEmailVerification) {
            EmailVerificationPanel(state, viewModel)
        } else {
            GoogleSignInButton(
                onIdToken = { viewModel.signInWithGoogle(it) },
                enabled = !state.loading
            )
            Spacer(modifier = Modifier.height(12.dp))
            AuthMethodTabs(selected = method, onSelect = { method = it })
            Spacer(modifier = Modifier.height(16.dp))
            if (method == 0) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.signInWithEmail(email, password) }, enabled = !state.loading) {
                    Text("Login")
                }
                TextButton(onClick = { showForgot = true }) { Text("Forgot password?") }
            } else {
                PhoneNumberField(
                    country = state.phoneCountry,
                    onCountryChange = viewModel::setPhoneCountry,
                    phone = phone,
                    onPhoneChange = { phone = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!state.otpSent) {
                    Button(onClick = { viewModel.sendOtp(null, state.phoneCountry.dialCode + phone, uiHost) }, enabled = !state.loading) {
                        Text("Send OTP")
                    }
                } else {
                    OutlinedTextField(
                        value = otp, onValueChange = { otp = it },
                        label = { Text("6-digit code") }, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.submitOtp(otp) }, enabled = !state.loading) {
                        Text("Verify & Login")
                    }
                    ResendCodeButton(
                        enabled = !state.loading,
                        onResend = { viewModel.sendOtp(null, state.phoneCountry.dialCode + phone, uiHost) }
                    )
                }
            }
        }

        AuthStatusMessages(state)

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onCreateAccount) { Text("New here? Create an account") }
    }
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onLogin: () -> Unit = {},
    /** needsProfile = first-time account → the profile-setup form comes before Home. */
    onRegisterSuccess: (needsProfile: Boolean) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val uiHost = rememberAuthUiHost()
    var method by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    LaunchedEffect(state.verifiedUser) { if (state.verifiedUser != null) onRegisterSuccess(state.needsProfile) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (state.awaitingEmailVerification) {
            EmailVerificationPanel(state, viewModel)
        } else {
            GoogleSignInButton(
                onIdToken = { viewModel.signInWithGoogle(it) },
                enabled = !state.loading
            )
            Spacer(modifier = Modifier.height(12.dp))
            AuthMethodTabs(selected = method, onSelect = { method = it })
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (method == 0) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password (min 6 characters)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.registerWithEmail(name, email, password) }, enabled = !state.loading) {
                    Text("Register")
                }
            } else {
                PhoneNumberField(
                    country = state.phoneCountry,
                    onCountryChange = viewModel::setPhoneCountry,
                    phone = phone,
                    onPhoneChange = { phone = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!state.otpSent) {
                    Button(onClick = { viewModel.sendOtp(name, state.phoneCountry.dialCode + phone, uiHost) }, enabled = !state.loading) {
                        Text("Send OTP")
                    }
                } else {
                    OutlinedTextField(
                        value = otp, onValueChange = { otp = it },
                        label = { Text("6-digit code") }, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.submitOtp(otp) }, enabled = !state.loading) {
                        Text("Verify & Register")
                    }
                    ResendCodeButton(
                        enabled = !state.loading,
                        onResend = { viewModel.sendOtp(name, state.phoneCountry.dialCode + phone, uiHost) }
                    )
                }
            }
        }

        AuthStatusMessages(state)

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onLogin) { Text("Already have an account? Log in") }
        // C2: consent surface at the point of account creation.
        var showLegal by remember { mutableStateOf(false) }
        if (showLegal) LegalDialog(onDismiss = { showLegal = false })
        TextButton(onClick = { showLegal = true }) {
            Text(
                "By continuing you agree to our Terms & Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val CITY_OTHER = "Other — type it"

/**
 * Reusable cascading address form (Country → State → City → PIN). State options populate from the
 * chosen country, city options from the chosen state (disabled until a state is picked), and city
 * offers an "Other" free-text fallback for unlisted places. PIN is free text validated per country.
 * Parent owns the values; picking a country resets state+city, picking a state resets city.
 */
@Composable
fun AddressForm(
    country: String?,
    stateRegion: String,
    city: String,
    pincode: String,
    addressLine: String,
    onCountryChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onPincodeChange: (String) -> Unit,
    onAddressLineChange: (String) -> Unit,
    pincodeError: Boolean = false
) {
    var showCountryPicker by remember { mutableStateOf(false) }
    var stateExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    val states = remember(country) { com.shoppilist.shared.domain.LocationData.statesFor(country) }
    val cities = remember(country, stateRegion) {
        com.shoppilist.shared.domain.LocationData.citiesFor(country, stateRegion)
    }
    // City is "custom" (free text) when the user picked Other or typed something not in the list.
    var cityIsCustom by remember(country, stateRegion) {
        mutableStateOf(city.isNotBlank() && city !in cities)
    }

    val currentCountry = com.shoppilist.shared.domain.CountryLanguageData.countries.find { it.code == country }
    // A read-only text field ignores taps on its own; disable it and overlay a clickable Box so the
    // whole field reliably opens the country dialog (keeps the enabled look via colour overrides).
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = currentCountry?.let { "${it.flag} ${it.name}" } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Country") },
            placeholder = { Text("Select country") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier.matchParentSize().clickable { showCountryPicker = true })
    }
    if (showCountryPicker) {
        Dialog(onDismissRequest = { showCountryPicker = false }) {
            Card {
                CountryPickerList(
                    modifier = Modifier.padding(16.dp).heightIn(max = 480.dp),
                    onSelect = { picked ->
                        onCountryChange(picked.code)
                        showCountryPicker = false
                    }
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    // State dropdown — enabled once a country with known states is selected.
    ExposedDropdownMenuBox(
        expanded = stateExpanded && states.isNotEmpty(),
        onExpandedChange = { if (states.isNotEmpty()) stateExpanded = it }
    ) {
        OutlinedTextField(
            value = stateRegion,
            onValueChange = {},
            readOnly = true,
            enabled = states.isNotEmpty(),
            label = { Text(if (states.isEmpty()) "State (select a country first)" else "State") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = stateExpanded, onDismissRequest = { stateExpanded = false }) {
            states.forEach { s ->
                DropdownMenuItem(text = { Text(s) }, onClick = {
                    onStateChange(s)
                    cityIsCustom = false
                    stateExpanded = false
                })
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    // City dropdown — disabled until a state is chosen; "Other" reveals a free-text field.
    val cityEnabled = stateRegion.isNotBlank()
    if (cityIsCustom) {
        OutlinedTextField(
            value = city,
            onValueChange = onCityChange,
            label = { Text("City") },
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = { cityIsCustom = false; onCityChange("") }) { Text("List") }
            },
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        ExposedDropdownMenuBox(
            expanded = cityExpanded && cityEnabled,
            onExpandedChange = { if (cityEnabled) cityExpanded = it }
        ) {
            OutlinedTextField(
                value = city,
                onValueChange = {},
                readOnly = true,
                enabled = cityEnabled,
                label = { Text(if (!cityEnabled) "City (select a state first)" else "City") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                cities.forEach { c ->
                    DropdownMenuItem(text = { Text(c) }, onClick = { onCityChange(c); cityExpanded = false })
                }
                DropdownMenuItem(
                    text = { Text(CITY_OTHER) },
                    onClick = { cityIsCustom = true; onCityChange(""); cityExpanded = false }
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = pincode,
        onValueChange = onPincodeChange,
        label = { Text("Postal / PIN / ZIP code") },
        singleLine = true,
        isError = pincodeError,
        supportingText = if (pincodeError) {
            { Text("Enter a valid code for the selected country") }
        } else null,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = addressLine,
        onValueChange = onAddressLineChange,
        label = { Text("Address line (optional)") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * First-run registration form: first name required; last name, email (hidden for email
 * sign-ups — the account already has one), and address optional. "Use my current location"
 * autofills the address from GPS and remembers the location for the dashboard.
 */
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onDone: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.load() }
    val state by viewModel.state.collectAsState()

    var firstName by remember(state.loading) { mutableStateOf(state.initialFirstName) }
    var lastName by remember(state.loading) { mutableStateOf(state.initialLastName) }
    var email by remember(state.loading) { mutableStateOf(state.initialEmail) }
    var address by remember(state.loading) { mutableStateOf(state.initialAddress) }
    var city by remember(state.loading) { mutableStateOf(state.initialCity) }
    var stateRegion by remember(state.loading) { mutableStateOf(state.initialState) }
    var pincode by remember(state.loading) { mutableStateOf(state.initialPincode) }
    var country by remember(state.loading) { mutableStateOf(state.initialCountry) }
    var pincodeError by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<StoredLocation?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var fetchingLocation by remember { mutableStateOf(false) }

    val locationController = rememberLocationController(
        onLocation = { fetched ->
            fetchingLocation = false
            locationError = null
            location = fetched
            // GPS reverse-geocode fills the granular fields.
            fetched.addressLine?.let { address = it }
            fetched.countryCode?.let { country = it }
            fetched.state?.let { if (stateRegion.isBlank()) stateRegion = it }
            fetched.city?.let { if (city.isBlank()) city = it }
        },
        onError = { message ->
            fetchingLocation = false
            locationError = message
        }
    )

    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tell us about you", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Just your first name is required — the rest is optional.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = firstName, onValueChange = { firstName = it },
            label = { Text("First name *") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = lastName, onValueChange = { lastName = it },
            label = { Text("Last name (optional)") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (!state.emailLocked) {
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email (optional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        AddressForm(
            country = country,
            stateRegion = stateRegion,
            city = city,
            pincode = pincode,
            addressLine = address,
            onCountryChange = { country = it; stateRegion = ""; city = "" },
            onStateChange = { stateRegion = it; city = "" },
            onCityChange = { city = it },
            onPincodeChange = { pincode = it; pincodeError = false },
            onAddressLineChange = { address = it },
            pincodeError = pincodeError
        )
        if (locationController.isAvailable) {
            TextButton(
                onClick = {
                    locationError = null
                    fetchingLocation = true
                    locationController.requestLocation()
                },
                enabled = !fetchingLocation
            ) {
                Text(if (fetchingLocation) "Locating…" else "📍 Use my current location")
            }
        }
        locationError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        location?.let { loc ->
            val place = listOfNotNull(loc.city, loc.state, loc.countryCode).joinToString(", ")
            if (place.isNotBlank()) {
                Text(
                    "Detected: $place",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                // PIN is optional, but if entered it must match the country's rule.
                if (pincode.isNotBlank() &&
                    !com.shoppilist.shared.domain.LocationData.isValidPincode(country, pincode)) {
                    pincodeError = true
                } else {
                    viewModel.save(firstName, lastName, email, address, city, stateRegion, pincode, country, location)
                }
            },
            enabled = !state.saving && !state.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.saving) "Saving…" else "Continue")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: com.shoppilist.shared.presentation.SettingsViewModel = koinViewModel(),
    onBack: () -> Unit,
    onOpenAdmin: () -> Unit = {},
    onLoggedOut: () -> Unit = {}
) {
    LaunchedEffect(Unit) { viewModel.load() }
    val user by viewModel.user.collectAsState()
    val authUser by viewModel.authUser.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()
    val profileSaved by viewModel.profileSaved.collectAsState()

    // Editable fields (item 3 / P2), re-seeded whenever the loaded user changes.
    var editName by remember(user?.userId) { mutableStateOf(user?.fullName.orEmpty()) }
    var editCountry by remember(user?.userId) { mutableStateOf(user?.countryCode) }
    var editState by remember(user?.userId) { mutableStateOf(user?.state.orEmpty()) }
    var editCity by remember(user?.userId) { mutableStateOf(user?.city.orEmpty()) }
    var editPincode by remember(user?.userId) { mutableStateOf(user?.pincode.orEmpty()) }
    var editAddress by remember(user?.userId) { mutableStateOf(user?.address.orEmpty()) }
    var pincodeError by remember { mutableStateOf(false) }

    // Add-missing-contact flows (email/phone with verification).
    val accountInfo by viewModel.accountInfo.collectAsState()
    val accountError by viewModel.accountError.collectAsState()
    val addPhoneOtpSent by viewModel.addPhoneOtpSent.collectAsState()
    val uiHost = rememberAuthUiHost()
    var showAddEmail by remember { mutableStateOf(false) }
    var showAddPhone by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    var showLegal by remember { mutableStateOf(false) }
    val deletingAccount by viewModel.deletingAccount.collectAsState()

    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }
    LaunchedEffect(profileSaved) { if (profileSaved) viewModel.ackProfileSaved() }
    // Auto-close the add dialogs once the account actually carries the new contact.
    LaunchedEffect(authUser?.email) { if (authUser?.email != null) showAddEmail = false }
    LaunchedEffect(authUser?.phoneNumber) {
        if (!authUser?.phoneNumber.isNullOrBlank()) showAddPhone = false
    }

    if (showAddEmail) {
        AddEmailDialog(
            error = accountError,
            onAdd = { email, password -> viewModel.addEmail(email, password) },
            onDismiss = { showAddEmail = false }
        )
    }
    if (showAddPhone) {
        AddPhoneDialog(
            defaultCountry = com.shoppilist.shared.domain.CountryLanguageData.countryFor(user?.countryCode)
                ?: com.shoppilist.shared.domain.CountryLanguageData.defaultCountry,
            otpSent = addPhoneOtpSent,
            error = accountError,
            onSendCode = { fullNumber -> viewModel.startAddPhone(fullNumber, uiHost) },
            onSubmitCode = { viewModel.submitAddPhoneOtp(it) },
            onDismiss = { showAddPhone = false; viewModel.resetAddPhone() }
        )
    }
    if (showChangePassword) {
        ChangePasswordDialog(
            info = accountInfo,
            error = accountError,
            onChange = { current, new -> viewModel.changePassword(current, new) },
            onDismiss = { showChangePassword = false }
        )
    }
    if (showDeleteAccount) {
        DeleteAccountDialog(
            deleting = deletingAccount,
            error = accountError,
            onConfirm = { viewModel.deleteAccount() },
            onDismiss = { if (!deletingAccount) showDeleteAccount = false }
        )
    }
    if (showLegal) {
        LegalDialog(onDismiss = { showLegal = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Profile header: avatar + name (screenshot b).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            val name = user?.fullName?.takeIf { it.isNotBlank() }
            com.shoppilist.shared.ui.components.ProfileAvatar(
                initial = name?.firstOrNull()?.toString(),
                seed = user?.userId ?: "me",
                size = 64.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                name ?: "Your profile",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Account & verification status (issue 8) + add-missing-contact flows.
        Text("Account", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                val account = authUser
                if (account == null) {
                    Text(
                        "Not signed in with a verified account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AccountContactRow(
                        label = "Email",
                        value = account.email,
                        verified = account.emailVerified,
                        action = {
                            if (account.email == null) {
                                TextButton(onClick = { showAddEmail = true }) { Text("Add") }
                            } else if (!account.emailVerified) {
                                TextButton(onClick = { viewModel.sendEmailVerification() }) { Text("Verify") }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AccountContactRow(
                        label = "Phone",
                        value = account.phoneNumber,
                        verified = !account.phoneNumber.isNullOrBlank(),
                        action = {
                            if (account.phoneNumber.isNullOrBlank()) {
                                TextButton(onClick = { showAddPhone = true }) { Text("Add") }
                            }
                        }
                    )
                    accountInfo?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    accountError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    // Re-check after clicking the emailed verification link.
                    if (account.email != null && !account.emailVerified) {
                        TextButton(onClick = { viewModel.refreshAccountStatus() }) { Text("I've verified — refresh") }
                    }
                }
            }
        }

        if (isAdmin) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Admin", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), onClick = onOpenAdmin) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Review user-added items", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Approve or reject items users typed that aren't in the master catalog",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Your details", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = editName, onValueChange = { editName = it },
            label = { Text("Name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        AddressForm(
            country = editCountry,
            stateRegion = editState,
            city = editCity,
            pincode = editPincode,
            addressLine = editAddress,
            onCountryChange = { editCountry = it; editState = ""; editCity = "" },
            onStateChange = { editState = it; editCity = "" },
            onCityChange = { editCity = it },
            onPincodeChange = { editPincode = it; pincodeError = false },
            onAddressLineChange = { editAddress = it },
            pincodeError = pincodeError
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (editPincode.isNotBlank() &&
                    !com.shoppilist.shared.domain.LocationData.isValidPincode(editCountry, editPincode)) {
                    pincodeError = true
                } else {
                    viewModel.saveProfile(editName, editCountry, editState, editCity, editPincode, editAddress)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (profileSaved) "Saved ✓" else "Save changes")
        }

        // Security actions: password change (email accounts) + sign out.
        if (authUser?.email != null) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { showChangePassword = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change password")
            }
        }
        Spacer(modifier = Modifier.height(if (authUser?.email != null) 8.dp else 24.dp))
        OutlinedButton(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log out")
        }

        // Danger zone: permanent account deletion (Play account-deletion policy).
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = { showDeleteAccount = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete account")
        }

        // Legal footer.
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = { showLegal = true }) {
                Text("Privacy Policy & Terms", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

