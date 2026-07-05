package com.shoppilist.shared.voice

sealed class VoiceIntent {
    data class CreateList(val name: String) : VoiceIntent()
    data class AddItem(val listName: String?, val itemName: String) : VoiceIntent()
    data class DeleteItem(val listName: String?, val itemName: String) : VoiceIntent()
    data class MarkPurchased(val listName: String?, val itemName: String) : VoiceIntent()
    object Unknown : VoiceIntent()
}

sealed class VoiceIntentResult {
    data class Success(val intent: VoiceIntent, val rawText: String) : VoiceIntentResult()
    data class Failure(val errorMessage: String, val rawText: String) : VoiceIntentResult()
}

interface VoiceIntentProcessor {
    suspend fun process(text: String): VoiceIntentResult
}

class RuleBasedProcessor : VoiceIntentProcessor {
    override suspend fun process(text: String): VoiceIntentResult {
        val lower = text.trim().lowercase()
        return try {
            when {
                lower.startsWith("create shopping list") || lower.startsWith("create list") -> {
                    // "Create shopping list called Monthly Grocery"
                    val after = lower.substringAfter("called", "").trim()
                    val name = if (after.isNotEmpty()) after else lower.substringAfter("create shopping list", "").trim()
                    VoiceIntentResult.Success(VoiceIntent.CreateList(name), text)
                }
                lower.startsWith("add ") && lower.contains(" to ") -> {
                    val after = lower.removePrefix("add ")
                    val parts = after.split(" to ")
                    val item = parts[0].trim()
                    val list = parts.getOrNull(1)?.trim()
                    VoiceIntentResult.Success(VoiceIntent.AddItem(list, item), text)
                }
                lower.startsWith("remove ") && lower.contains(" from ") -> {
                    val after = lower.removePrefix("remove ")
                    val parts = after.split(" from ")
                    val item = parts[0].trim()
                    val list = parts.getOrNull(1)?.trim()
                    VoiceIntentResult.Success(VoiceIntent.DeleteItem(list, item), text)
                }
                lower.startsWith("mark ") && lower.contains(" as purchased") -> {
                    val item = lower.removePrefix("mark ").removeSuffix(" as purchased").trim()
                    VoiceIntentResult.Success(VoiceIntent.MarkPurchased(null, item), text)
                }
                else -> VoiceIntentResult.Success(VoiceIntent.Unknown, text)
            }
        } catch (e: Exception) {
            VoiceIntentResult.Failure(e.message ?: "parse_error", text)
        }
    }
}

