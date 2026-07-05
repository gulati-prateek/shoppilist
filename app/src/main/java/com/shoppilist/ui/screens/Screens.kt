package com.shoppilist.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.koin.androidx.compose.koinViewModel
import com.shoppilist.shared.presentation.AuthViewModel
import com.shoppilist.shared.presentation.ListDetailViewModel

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onTimeout()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ShoppiList", style = MaterialTheme.typography.displaySmall)
            Text("Shop together, anywhere.", style = MaterialTheme.typography.bodyMedium)
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
        Text("Shop together, anywhere.")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNext) { Text("Get Started") }
    }
}

@Composable
fun LoginScreen(viewModel: AuthViewModel = koinViewModel(), onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.loginOrRegister(fullName = null, email = email, phone = null, onDone = onLoginSuccess) }) {
            Text("Login")
        }
    }
}

@Composable
fun RegisterScreen(viewModel: AuthViewModel = koinViewModel(), onRegisterSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create Profile", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") })
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.loginOrRegister(fullName = name, email = email, phone = null, onDone = onRegisterSuccess) }) {
            Text("Register")
        }
    }
}

@Composable
fun AddItemScreen(listId: String, viewModel: ListDetailViewModel = koinViewModel(), onBack: () -> Unit) {
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Add Item", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Item Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it },
                label = { Text("Unit") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (itemName.isNotBlank()) {
                    viewModel.addItem(
                        listId,
                        itemName.trim(),
                        quantity.toDoubleOrNull() ?: 1.0,
                        unit.trim().ifBlank { null },
                        notes.trim().ifBlank { null }
                    )
                    onBack()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add")
        }
    }
}

@Composable
fun VoiceScreen(onBack: () -> Unit, viewModel: com.shoppilist.shared.presentation.VoiceViewModel = org.koin.androidx.compose.koinViewModel()) {
    val input by viewModel.inputText.collectAsState()
    val result by viewModel.result.collectAsState()
    val context = LocalContext.current

    var isListening by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer?.destroy() }
    }

    fun startListening() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            speechError = "Speech recognition isn't available on this device"
            return
        }
        speechError = null
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                speechError = "Didn't catch that, try again"
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    viewModel.updateInput(text)
                    viewModel.processText(text)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else speechError = "Microphone permission is needed for voice input"
    }

    fun onMicTapped() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) startListening() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: com.shoppilist.shared.presentation.SettingsViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.load() }
    val user by viewModel.user.collectAsState()
    var countryExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(modifier = Modifier.height(16.dp))

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
        ExposedDropdownMenuBox(expanded = countryExpanded, onExpandedChange = { countryExpanded = it }) {
            OutlinedTextField(
                value = currentCountry?.let { "${it.flag} ${it.name}" } ?: "Not set",
                onValueChange = {},
                readOnly = true,
                label = { Text("Country") },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                com.shoppilist.shared.domain.CountryLanguageData.countries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text("${country.flag} ${country.name}") },
                        onClick = {
                            val defaultLanguage = com.shoppilist.shared.domain.CountryLanguageData.languagesFor(country.code).firstOrNull()?.code ?: "en"
                            viewModel.setLocale(country.code, defaultLanguage)
                            countryExpanded = false
                        }
                    )
                }
            }
        }
    }
}

