package com.shoppilist.voice

import com.shoppilist.shared.domain.*
import com.shoppilist.shared.data.local.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * CommandExecutor maps VoiceIntent to domain UseCases.
 * Handles context resolution (list name -> listId) and executes DB operations.
 */
class CommandExecutor(
    private val getAllListsUseCase: GetAllListsUseCase,
    private val getListUseCase: GetListUseCase,
    private val createListUseCase: CreateListUseCase,
    private val addItemUseCase: AddItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val markItemCheckedUseCase: MarkItemCheckedUseCase,
    private val getListItemsUseCase: GetListItemsUseCase,
    private val deleteListUseCase: DeleteListUseCase,
    private val currentUserId: String
) {

    private val _executionResult = MutableStateFlow<ExecutionResult?>(null)
    val executionResult = _executionResult.asStateFlow()

    suspend fun execute(intent: VoiceIntent): ExecutionResult {
        val result = when (intent) {
            is VoiceIntent.CreateList -> handleCreateList(intent)
            is VoiceIntent.AddItem -> handleAddItem(intent)
            is VoiceIntent.DeleteItem -> handleDeleteItem(intent)
            is VoiceIntent.MarkPurchased -> handleMarkPurchased(intent)
            is VoiceIntent.Unknown -> ExecutionResult.Failure("Unknown intent")
            else -> ExecutionResult.Failure("Unsupported intent")
        }
        _executionResult.value = result
        return result
    }

    private suspend fun handleCreateList(intent: VoiceIntent.CreateList): ExecutionResult {
        return try {
            val result = createListUseCase(intent.name, description = null, ownerId = currentUserId)
            if (result.isSuccess) {
                ExecutionResult.Success("List '${intent.name}' created")
            } else {
                ExecutionResult.Failure(result.exceptionOrNull()?.message ?: "Failed to create list")
            }
        } catch (e: Exception) {
            ExecutionResult.Failure(e.message ?: "Create list error")
        }
    }

    private suspend fun handleAddItem(intent: VoiceIntent.AddItem): ExecutionResult {
        return try {
            val listId = resolveListId(intent.listName) ?: return ExecutionResult.Failure("List not found")
            val result = addItemUseCase(
                listId = listId,
                itemName = intent.itemName,
                quantity = 1.0,
                unit = null,
                category = null,
                addedBy = currentUserId
            )
            if (result.isSuccess) {
                ExecutionResult.Success("Added '${intent.itemName}' to list")
            } else {
                ExecutionResult.Failure(result.exceptionOrNull()?.message ?: "Failed to add item")
            }
        } catch (e: Exception) {
            ExecutionResult.Failure(e.message ?: "Add item error")
        }
    }

    private suspend fun handleDeleteItem(intent: VoiceIntent.DeleteItem): ExecutionResult {
        return try {
            val listId = intent.listName?.let { resolveListId(it) } ?: return ExecutionResult.Failure("List ambiguous")
            val items = getListItemsUseCase(listId)
            // For testing: just delete first item by name match
            val itemId = intent.itemName // simplified; in production, match by name
            val result = deleteItemUseCase(itemId)
            if (result.isSuccess) {
                ExecutionResult.Success("Removed '${intent.itemName}' from list")
            } else {
                ExecutionResult.Failure(result.exceptionOrNull()?.message ?: "Failed to delete item")
            }
        } catch (e: Exception) {
            ExecutionResult.Failure(e.message ?: "Delete item error")
        }
    }

    private suspend fun handleMarkPurchased(intent: VoiceIntent.MarkPurchased): ExecutionResult {
        return try {
            val listId = intent.listName?.let { resolveListId(it) } ?: return ExecutionResult.Failure("List ambiguous")
            // Simplified: match by name; in production, search items in listId and mark first match
            val itemId = intent.itemName // simplified
            val result = markItemCheckedUseCase(itemId, checked = true)
            if (result.isSuccess) {
                ExecutionResult.Success("Marked '${intent.itemName}' as purchased")
            } else {
                ExecutionResult.Failure(result.exceptionOrNull()?.message ?: "Failed to mark item")
            }
        } catch (e: Exception) {
            ExecutionResult.Failure(e.message ?: "Mark item error")
        }
    }

    private suspend fun resolveListId(listName: String?): String? {
        if (listName == null) return null
        // Try exact match first (case-insensitive), then prefix match
        try {
            val lists = getAllListsUseCase()
            // collect a single snapshot (suspend until first emission)
            val snapshot = try { lists.first() } catch (e: Exception) { null }
            if (snapshot != null) {
                val lowerName = listName.lowercase()
                // exact match
                val exact = snapshot.firstOrNull { it.name.lowercase() == lowerName }
                if (exact != null) return exact.listId
                // contains/prefix match
                val contains = snapshot.firstOrNull { it.name.lowercase().contains(lowerName) }
                if (contains != null) return contains.listId
                val prefix = snapshot.firstOrNull { it.name.lowercase().startsWith(lowerName) }
                if (prefix != null) return prefix.listId
            }
        } catch (e: Exception) {
            // ignore and return null
        }
        return null
    }
}

sealed class ExecutionResult {
    data class Success(val message: String) : ExecutionResult()
    data class Failure(val error: String) : ExecutionResult()
}


