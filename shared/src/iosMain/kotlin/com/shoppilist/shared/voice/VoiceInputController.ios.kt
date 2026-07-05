package com.shoppilist.shared.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Placeholder until a real `SFSpeechRecognizer`/`AVAudioSession` integration is written and
 * tested against a real Mac/Xcode toolchain (Phase 6) — this file only needs to compile here,
 * its actual behavior can't be verified from a Windows CI compile check.
 */
@Composable
actual fun rememberVoiceInputController(
    onResult: (String) -> Unit,
    onListeningChanged: (Boolean) -> Unit,
    onError: (String) -> Unit
): VoiceInputController {
    return remember {
        object : VoiceInputController {
            override val isAvailable: Boolean = false

            override fun startListening() {
                onError("Voice input isn't available on iOS yet")
            }

            override fun destroy() {}
        }
    }
}
