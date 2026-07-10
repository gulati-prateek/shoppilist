package com.shoppilist.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
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
import com.shoppilist.shared.location.rememberLocationController
import com.shoppilist.shared.presentation.AuthUiState
import com.shoppilist.shared.presentation.AuthViewModel
import com.shoppilist.shared.presentation.ProfileViewModel
import com.shoppilist.shared.presentation.SplashViewModel
import com.shoppilist.shared.presentation.StartDestination
import com.shoppilist.shared.voice.rememberVoiceInputController

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = koinViewModel(),
    onResolved: (StartDestination) -> Unit
) {
    LaunchedEffect(Unit) {
        // Session restore + the verified gate run behind the brand splash; the short delay
        // keeps the splash from flashing when the check returns instantly (e.g. no session).
        val destination = viewModel.resolveStartDestination()
        kotlinx.coroutines.delay(800)
        onResolved(destination)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ShoppiList", style = MaterialTheme.typography.displaySmall)
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

/** One contact method (email/phone) with its verification badge (issue 8). */
@Composable
private fun AccountContactRow(label: String, value: String?, verified: Boolean) {
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

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onCreateAccount: () -> Unit = {},
    /** needsProfile = first-time account → the profile-setup form comes before Home. */
    onLoginSuccess: (needsProfile: Boolean) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val uiHost = rememberAuthUiHost()
    var method by remember { mutableStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    LaunchedEffect(state.verifiedUser) { if (state.verifiedUser != null) onLoginSuccess(state.needsProfile) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        if (state.awaitingEmailVerification) {
            EmailVerificationPanel(state, viewModel)
        } else {
            AuthMethodTabs(selected = method, onSelect = { method = it })
            Spacer(modifier = Modifier.height(16.dp))
            if (method == 0) {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") }, modifier = Modifier.fillMaxWidth()
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
            } else {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                    label = { Text("Phone number") },
                    prefix = { Text("+91 ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!state.otpSent) {
                    Button(onClick = { viewModel.sendOtp(null, "+91$phone", uiHost) }, enabled = !state.loading) {
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
                    TextButton(onClick = { viewModel.sendOtp(null, "+91$phone", uiHost) }, enabled = !state.loading) {
                        Text("Resend code")
                    }
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
                    label = { Text("Email") }, modifier = Modifier.fillMaxWidth()
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
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                    label = { Text("Phone number") },
                    prefix = { Text("+91 ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!state.otpSent) {
                    Button(onClick = { viewModel.sendOtp(name, "+91$phone", uiHost) }, enabled = !state.loading) {
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
                    TextButton(onClick = { viewModel.sendOtp(name, "+91$phone", uiHost) }, enabled = !state.loading) {
                        Text("Resend code")
                    }
                }
            }
        }

        AuthStatusMessages(state)

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onLogin) { Text("Already have an account? Log in") }
    }
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
    var location by remember { mutableStateOf<StoredLocation?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var fetchingLocation by remember { mutableStateOf(false) }

    val locationController = rememberLocationController(
        onLocation = { fetched ->
            fetchingLocation = false
            locationError = null
            location = fetched
            // GPS reverse-geocode fills the granular fields (screenshot c).
            fetched.addressLine?.let { address = it }
            fetched.city?.let { if (city.isBlank()) city = it }
            fetched.state?.let { if (stateRegion.isBlank()) stateRegion = it }
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
        OutlinedTextField(
            value = address, onValueChange = { address = it },
            label = { Text("Address line (optional)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = city, onValueChange = { city = it },
                label = { Text("City") }, singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = stateRegion, onValueChange = { stateRegion = it },
                label = { Text("State") }, singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pincode, onValueChange = { pincode = it },
            label = { Text("Postal / PIN code") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
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
            onClick = { viewModel.save(firstName, lastName, email, address, city, stateRegion, pincode, location) },
            enabled = !state.saving && !state.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.saving) "Saving…" else "Continue")
        }
    }
}

@Composable
fun VoiceScreen(onBack: () -> Unit, viewModel: com.shoppilist.shared.presentation.VoiceViewModel = koinViewModel()) {
    val input by viewModel.inputText.collectAsState()
    val result by viewModel.result.collectAsState()

    var isListening by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }

    val voiceInput = rememberVoiceInputController(
        onResult = { text ->
            viewModel.updateInput(text)
            viewModel.processText(text)
        },
        onListeningChanged = { isListening = it },
        onError = { speechError = it }
    )

    DisposableEffect(Unit) {
        onDispose { voiceInput.destroy() }
    }

    fun onMicTapped() {
        speechError = null
        voiceInput.startListening()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Voice Assistant", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        IconButton(onClick = { onMicTapped() }, modifier = Modifier.size(80.dp)) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Tap to speak",
                modifier = Modifier.size(80.dp),
                tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        if (isListening) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Listening…", style = MaterialTheme.typography.bodyMedium)
        }
        if (!voiceInput.isAvailable) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Voice input isn't available on this device — type a command below instead",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (speechError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(speechError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { viewModel.updateInput(it) },
            label = { Text("Or type a command") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { viewModel.processText(input) }) { Text("Process") }
            Button(onClick = { viewModel.updateInput("") }) { Text("Clear") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        result?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Try saying:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Create list called Weekly Groceries", style = MaterialTheme.typography.bodySmall)
                Text("• Add milk to Weekly Groceries", style = MaterialTheme.typography.bodySmall)
                Text("• Add milk  (goes to your latest list)", style = MaterialTheme.typography.bodySmall)
                Text("• Mark milk as purchased", style = MaterialTheme.typography.bodySmall)
                Text("• Remove milk from Weekly Groceries", style = MaterialTheme.typography.bodySmall)
            }
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
    var showCountryPicker by remember { mutableStateOf(false) }

    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }

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

        // Account & verification status (issue 8)
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
                    user?.let { profile ->
                        val fullAddress = listOfNotNull(
                            profile.address?.takeIf { it.isNotBlank() },
                            profile.city?.takeIf { it.isNotBlank() },
                            profile.state?.takeIf { it.isNotBlank() },
                            profile.pincode?.takeIf { it.isNotBlank() }
                        ).joinToString(", ")
                        if (fullAddress.isNotBlank()) {
                            ProfileInfoRow(label = "Address", value = fullAddress)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    AccountContactRow(label = "Email", value = account.email, verified = account.emailVerified)
                    Spacer(modifier = Modifier.height(8.dp))
                    AccountContactRow(
                        label = "Phone",
                        value = account.phoneNumber,
                        verified = !account.phoneNumber.isNullOrBlank()
                    )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Hide sponsored links", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Show only organic retailer recommendations in Order Online",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = user?.hideSponsoredLinks ?: false,
                onCheckedChange = { viewModel.setHideSponsoredLinks(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Country & Language", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        val currentCountry = com.shoppilist.shared.domain.CountryLanguageData.countries
            .find { it.code == user?.countryCode }
        OutlinedTextField(
            value = currentCountry?.let { "${it.flag} ${it.name}" } ?: "Not set",
            onValueChange = {},
            readOnly = true,
            label = { Text("Country") },
            modifier = Modifier.fillMaxWidth().clickable { showCountryPicker = true }
        )
        if (showCountryPicker) {
            Dialog(onDismissRequest = { showCountryPicker = false }) {
                Card {
                    CountryPickerList(
                        modifier = Modifier.padding(16.dp).heightIn(max = 480.dp),
                        onSelect = { country ->
                            val defaultLanguage = com.shoppilist.shared.domain.CountryLanguageData.languagesFor(country.code).firstOrNull()?.code ?: "en"
                            viewModel.setLocale(country.code, defaultLanguage)
                            showCountryPicker = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log out")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

