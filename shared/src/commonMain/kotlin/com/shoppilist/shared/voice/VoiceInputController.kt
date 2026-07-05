package com.shoppilist.shared.voice

import androidx.compose.runtime.Composable

/**
 * Abstracts platform speech recognition (`android.speech.SpeechRecognizer` has no Kotlin/Native
 * equivalent). The Android `actual` wraps the real `SpeechRecognizer` API (including the
 * RECORD_AUDIO runtime permission flow); the iOS `actual` is a placeholder that reports
 * unavailable until a real `SFSpeechRecognizer` integration is written against a real Mac/Xcode
 * toolchain (Phase 6) — this can't be verified from a Windows CI compile check alone.
 */
interface VoiceInputController {
    val isAvailable: Boolean
    fun startListening()
    fun destroy()
}

@Composable
expect fun rememberVoiceInputController(
    onResult: (String) -> Unit,
    onListeningChanged: (Boolean) -> Unit,
    onError: (String) -> Unit
): VoiceInputController
