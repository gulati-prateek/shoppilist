package com.shoppilist.shared.voice

import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.data.local.ShoppingListEntity
import com.shoppilist.shared.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
            val listId = if (intent.listName != null) {
                resolveListId(intent.listName)
                    ?: return ExecutionResult.Failure("Couldn't find a list named '${intent.listName}'")
            } else {
                listsSnapshot().firstOrNull()?.listId
                    ?: return ExecutionResult.Failure("No lists yet — try 'Create list called Weekly Groceries' first")
            }
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
            val item = findItemByName(intent.listName, intent.itemName)
                ?: return ExecutionResult.Failure(itemNotFoundMessage(intent.itemName, intent.listName))
            val result = deleteItemUseCase(item.itemId)
            if (result.isSuccess) {
                ExecutionResult.Success("Removed '${item.name}' from list")
            } else {
                ExecutionResult.Failure(result.exceptionOrNull()?.message ?: "Failed to delete item")
            }
        } catch (e: Exception) {
            ExecutionResult.Failure(e.message ?: "Delete item error")
        }
    }

    private suspend fun handleMarkPurchased(intent: VoiceIntent.MarkPurchased): ExecutionResult {
        return try {
            val item = findItemByName(intent.listName, intent.itemName)
                ?: return ExecutionResult.Failure(itemNotFoundMessage(intent.itemName, intent.listName))
            val result = markItemCheckedUseCase(item.itemId, checked = true)
            if (result.isSuccess) {
                ExecutionResult.Success("Marked '${item.name}' as purchased")
            } else {
                ExecutionResult.Failure(result.exceptionOrNull()?.message ?: "Failed to mark item")
            }
        } catch (e: Exception) {
            ExecutionResult.Failure(e.message ?: "Mark item error")
        }
    }

    private fun itemNotFoundMessage(itemName: String, listName: String?): String =
        if (listName != null) "Couldn't find '$itemName' in '$listName'" else "Couldn't find '$itemName' in your lists"

    private suspend fun listsSnapshot(): List<ShoppingListEntity> =
        try { getAllListsUseCase().first() } catch (e: Exception) { emptyList() }

    /** Finds an item by spoken name — in the named list if given, otherwise across all lists. */
    private suspend fun findItemByName(listName: String?, itemName: String): ShoppingItemEntity? {
        val listIds = if (listName != null) {
            listOfNotNull(resolveListId(listName))
        } else {
            listsSnapshot().map { it.listId }
        }
        val lower = itemName.lowercase()
        for (listId in listIds) {
            val items = try { getListItemsUseCase(listId).first() } catch (e: Exception) { emptyList() }
            val match = items.firstOrNull { it.name.lowercase() == lower }
                ?: items.firstOrNull { it.name.lowercase().contains(lower) }
            if (match != null) return match
        }
        return null
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


