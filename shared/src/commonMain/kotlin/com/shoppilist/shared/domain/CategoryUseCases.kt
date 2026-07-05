package com.shoppilist.shared.domain

import com.shoppilist.shared.data.repository.ItemCategoryRepository
import com.shoppilist.shared.data.repository.ShoppingItemRepository

class GetCategoriesForListUseCase(private val repo: ItemCategoryRepository) {
    operator fun invoke(listId: String) = repo.getCategoriesForList(listId)
}

/** Auto-categorize on add: silently apply a confident match, leave ambiguous/no-match items uncategorized. */
class AutoCategorizeItemUseCase(private val matcher: CategoryMatcher) {
    suspend operator fun invoke(itemName: String): CategoryMatch = matcher.match(itemName)
}

/** Manual override (§2.12): sets the item's category and records a correction so the whole family benefits. */
class OverrideItemCategoryUseCase(
    private val itemRepo: ShoppingItemRepository,
    private val categoryRepo: ItemCategoryRepository
) {
    suspend operator fun invoke(itemId: String, itemName: String, previousSuggestion: String?, newCategoryId: String, correctedBy: String): Result<Unit> {
        categoryRepo.recordCorrection(itemName, previousSuggestion, newCategoryId, correctedBy)
        return itemRepo.updateCategory(itemId, newCategoryId, correctedBy)
    }
}

class RenameCategoryForListUseCase(private val repo: ItemCategoryRepository) {
    suspend operator fun invoke(listId: String, sourceCategoryId: String, newName: String): Result<String> =
        repo.renameCategoryForList(listId, sourceCategoryId, newName)
}

class CreateCustomCategoryUseCase(private val repo: ItemCategoryRepository) {
    suspend operator fun invoke(listId: String, name: String, emoji: String, displayOrder: Int): Result<String> =
        repo.createCustomCategory(listId, name, emoji, displayOrder)
}
