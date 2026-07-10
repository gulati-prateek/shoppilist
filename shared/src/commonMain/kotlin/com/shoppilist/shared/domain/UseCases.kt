@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.domain

import com.shoppilist.shared.backend.CatalogBackend
import com.shoppilist.shared.data.local.ShoppingListEntity
import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.repository.ShoppingListRepository
import com.shoppilist.shared.data.repository.ShoppingItemRepository
import com.shoppilist.shared.data.repository.SuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

// List use cases
class GetAllListsUseCase(private val repo: ShoppingListRepository) {
    operator fun invoke(): Flow<List<ShoppingListEntity>> = repo.getAllLists()
}

class GetListUseCase(private val repo: ShoppingListRepository) {
    operator fun invoke(listId: String): Flow<ShoppingListEntity?> = repo.getListFlow(listId)
}

class CreateListUseCase(private val repo: ShoppingListRepository) {
    suspend operator fun invoke(name: String, description: String?, ownerId: String, colorHex: String? = null): Result<String> {
        val list = ShoppingListEntity(
            listId = Uuid.random().toString(),
            name = name,
            description = description,
            ownerId = ownerId,
            householdId = null,
            colorHex = colorHex
        )
        return repo.createList(list)
    }
}

class DeleteListUseCase(private val repo: ShoppingListRepository) {
    suspend operator fun invoke(listId: String): Result<Unit> = repo.deleteList(listId)
}

class ArchiveListUseCase(private val repo: ShoppingListRepository) {
    suspend operator fun invoke(listId: String): Result<Unit> = repo.archiveList(listId)
}

class TogglePinUseCase(private val repo: ShoppingListRepository) {
    suspend operator fun invoke(listId: String, pinned: Boolean): Result<Unit> = repo.setPinned(listId, pinned)
}

// Item use cases
class GetListItemsUseCase(private val repo: ShoppingItemRepository) {
    operator fun invoke(listId: String): Flow<List<ShoppingItemEntity>> = repo.getItemsForList(listId)
}

class GetItemOnceUseCase(private val repo: ShoppingItemRepository) {
    suspend operator fun invoke(itemId: String): ShoppingItemEntity? = repo.getItemOnce(itemId)
}

/** Result of adding an item: the new item id, plus a low-confidence category suggestion the UI
 *  should confirm with the user (§2.12), if the matcher couldn't categorize it silently. */
data class AddItemResult(val itemId: String, val ambiguousCategoryId: String? = null)

class AddItemUseCase(
    private val repo: ShoppingItemRepository,
    private val categoryMatcher: CategoryMatcher,
    private val suggestionRepo: SuggestionRepository,
    private val catalogBackend: CatalogBackend,
    private val userDao: UserDao
) {
    suspend operator fun invoke(
        listId: String,
        itemName: String,
        quantity: Double = 1.0,
        unit: String? = null,
        category: String? = null,
        notes: String? = null,
        addedBy: String? = null
    ): Result<AddItemResult> {
        val match = categoryMatcher.match(itemName)
        val categoryId = (match as? CategoryMatch.Confident)?.categoryId
        val ambiguousCategoryId = (match as? CategoryMatch.Ambiguous)?.suggestedCategoryId

        val item = ShoppingItemEntity(
            itemId = Uuid.random().toString(),
            listId = listId,
            name = itemName,
            quantity = quantity,
            unit = unit,
            category = category,
            categoryId = categoryId,
            notes = notes,
            addedBy = addedBy
        )
        val result = repo.addItem(item)
        if (result.isSuccess && addedBy != null) {
            suggestionRepo.recordItemAdded(addedBy, itemName, listId)
            // Every add path (create-list picker, list-detail field, voice) funnels through
            // here, so this one hook reports all off-catalog items for admin review.
            if (match is CategoryMatch.NoMatch) {
                val user = userDao.getUserOnce(addedBy)
                catalogBackend.reportCustomItem(
                    name = itemName,
                    userId = addedBy,
                    userName = user?.fullName,
                    countryCode = user?.countryCode
                )
            }
        }
        return result.map { AddItemResult(it, ambiguousCategoryId) }
    }
}

class UpdateItemUseCase(private val repo: ShoppingItemRepository) {
    suspend operator fun invoke(item: ShoppingItemEntity): Result<Unit> = repo.updateItem(item)
}

class DeleteItemUseCase(private val repo: ShoppingItemRepository) {
    suspend operator fun invoke(itemId: String): Result<Unit> = repo.deleteItem(itemId)
}

class MarkItemCheckedUseCase(private val repo: ShoppingItemRepository) {
    suspend operator fun invoke(itemId: String, checked: Boolean): Result<Unit> =
        repo.markItemChecked(itemId, checked)
}

class ClearPurchasedUseCase(private val repo: ShoppingItemRepository) {
    suspend operator fun invoke(listId: String): Result<Unit> = repo.clearPurchased(listId)
}

