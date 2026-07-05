package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.voice.CommandExecutor
import com.shoppilist.shared.voice.VoiceIntentProcessor
import com.shoppilist.shared.voice.VoiceIntentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VoiceViewModel(
    private val processor: VoiceIntentProcessor,
    private val executor: CommandExecutor
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result

    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun processText(text: String) {
        viewModelScope.launch {
            val parsed = processor.process(text)
            if (parsed is VoiceIntentResult.Success) {
                val exec = executor.execute(parsed.intent)
                when (exec) {
                    is com.shoppilist.shared.voice.ExecutionResult.Success -> _result.value = exec.message
                    is com.shoppilist.shared.voice.ExecutionResult.Failure -> _result.value = "Error: ${exec.error}"
                }
            } else if (parsed is VoiceIntentResult.Failure) {
                _result.value = "Parse error: ${parsed.errorMessage}"
            }
        }
    }
}

