@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.domain

import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.data.local.ShoppingListEntity
import com.shoppilist.shared.data.repository.ShoppingItemRepository
import com.shoppilist.shared.data.repository.ShoppingListRepository
import kotlin.uuid.Uuid

/** Groups selected items into a new sub-list nested under the parent (§2.6). */
class CreateSubListUseCase(
    private val listRepo: ShoppingListRepository,
    private val itemRepo: ShoppingItemRepository
) {
    suspend operator fun invoke(parentListId: String, name: String, ownerId: String, itemIds: List<String>): Result<String> {
        val parent = listRepo.getListOnce(parentListId) ?: return Result.failure(IllegalArgumentException("Parent list not found"))
        val subList = ShoppingListEntity(
            listId = Uuid.random().toString(),
            name = name,
            description = null,
            ownerId = ownerId,
            householdId = parent.householdId,
            parentListId = parentListId
        )
        val createResult = listRepo.createList(subList)
        if (createResult.isFailure) return Result.failure(createResult.exceptionOrNull()!!)
        itemRepo.moveItemsToList(itemIds, subList.listId)
        return Result.success(subList.listId)
    }
}

/** Promotes a sub-list to a standalone list (§2.6). */
class PromoteSubListUseCase(private val listRepo: ShoppingListRepository) {
    suspend operator fun invoke(listId: String): Result<Unit> = listRepo.setParent(listId, null)
}

/** "Done Shopping" (§2.7): returns any items still unchecked so the UI can offer to save them as a leftover list. */
class DoneShoppingUseCase(private val itemRepo: ShoppingItemRepository) {
    suspend operator fun invoke(listId: String): List<ShoppingItemEntity> = itemRepo.getUncheckedItemsOnce(listId)
}

class CreateLeftoverListUseCase(
    private val listRepo: ShoppingListRepository,
    private val itemRepo: ShoppingItemRepository
) {
    suspend operator fun invoke(sourceListId: String, sourceListName: String, ownerId: String, leftoverName: String? = null): Result<String> {
        val source = listRepo.getListOnce(sourceListId)
        val unchecked = itemRepo.getUncheckedItemsOnce(sourceListId)
        if (unchecked.isEmpty()) return Result.failure(IllegalStateException("No leftover items"))

        val newList = ShoppingListEntity(
            listId = Uuid.random().toString(),
            name = leftoverName ?: "Leftover from $sourceListName",
            description = null,
            ownerId = ownerId,
            householdId = source?.householdId
        )
        val createResult = listRepo.createList(newList)
        if (createResult.isFailure) return Result.failure(createResult.exceptionOrNull()!!)
        itemRepo.moveItemsToList(unchecked.map { it.itemId }, newList.listId)
        return Result.success(newList.listId)
    }
}

/** Merges a leftover (or any) list into an existing target list, then removes the source (§2.7). */
class MergeListsUseCase(
    private val listRepo: ShoppingListRepository,
    private val itemRepo: ShoppingItemRepository
) {
    suspend operator fun invoke(sourceListId: String, targetListId: String): Result<Unit> {
        return try {
            val sourceItems = itemRepo.getItemsForListOnce(sourceListId)
            if (sourceItems.isNotEmpty()) {
                itemRepo.moveItemsToList(sourceItems.map { it.itemId }, targetListId)
            }
            listRepo.deleteList(sourceListId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
