package com.shoppilist.shared.domain

import com.shoppilist.shared.data.repository.ItemCategoryRepository

sealed class CategoryMatch {
    data class Confident(val categoryId: String) : CategoryMatch()
    data class Ambiguous(val suggestedCategoryId: String) : CategoryMatch()
    object NoMatch : CategoryMatch()
}

/**
 * Auto-categorizes an item name for the aisle/category grouping feature (§2.12). Checks
 * family corrections first (so a manual fix "learns" for everyone), then the curated global
 * item catalog. A correction is always confident since a human already resolved it; a global
 * catalog hit is confident only when the name matches exactly (a substring match is treated as
 * ambiguous and surfaced to the user, e.g. "Is 'Dettol' Household or Health?").
 */
class CategoryMatcher(private val categoryRepository: ItemCategoryRepository) {

    suspend fun match(itemName: String): CategoryMatch {
        val normalized = itemName.trim().lowercase()
        if (normalized.isEmpty()) return CategoryMatch.NoMatch

        categoryRepository.getLatestCorrection(normalized)?.let {
            return CategoryMatch.Confident(it.correctCategoryId)
        }

        categoryRepository.findGlobalItemByName(normalized)?.let {
            return CategoryMatch.Confident(it.categoryId)
        }

        // No exact match: a prefix hit against the catalog is treated as low-confidence, so the
        // caller can prompt the user to confirm rather than silently guessing wrong.
        val firstWord = normalized.substringBefore(' ')
        categoryRepository.searchGlobalItemsByPrefix(firstWord, limit = 1).firstOrNull()?.let {
            return CategoryMatch.Ambiguous(it.categoryId)
        }

        return CategoryMatch.NoMatch
    }
}
