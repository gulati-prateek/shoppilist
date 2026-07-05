package com.shoppilist.shared.domain

import com.shoppilist.shared.data.repository.ShoppingItemRepository
import com.shoppilist.shared.data.repository.ShoppingListRepository
import com.shoppilist.shared.data.repository.SuggestionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Suggestion chips (§2.8): merges per-user purchase frequency with the curated global catalog
 * filtered by country and current-month seasonality, dedups, ranks history first, and demotes
 * anything the user has dismissed before.
 */
class SuggestionEngine(private val suggestionRepo: SuggestionRepository) {

    suspend fun getSuggestions(userId: String, countryCode: String?, queryPrefix: String = "", limit: Int = 8): List<String> {
        val dismissals = suggestionRepo.getDismissalCounts(userId)

        val historyMatches = if (queryPrefix.isBlank()) {
            suggestionRepo.getTopHistoryItems(userId, limit = limit * 2).map { it.itemName }
        } else {
            suggestionRepo.searchHistory(userId, queryPrefix, limit = limit * 2).map { it.itemName }
        }

        val catalogMatches = suggestionRepo.searchGlobalItems(queryPrefix, limit = limit * 2)
            .filter { countryCode == null || it.countryCodes.isEmpty() || it.countryCodes.contains(countryCode) }
            .map { it.name }

        val seasonalMatches = if (queryPrefix.isBlank()) {
            val currentMonth = currentMonthCode()
            suggestionRepo.getSeasonalItems()
                .filter { it.seasonMonths.contains(currentMonth) }
                .filter { countryCode == null || it.countryCodes.isEmpty() || it.countryCodes.contains(countryCode) }
                .map { it.name }
        } else emptyList()

        // History first, then catalog, then seasonal — dedup keeping first occurrence, demote dismissed.
        val ranked = (historyMatches + catalogMatches + seasonalMatches)
            .distinct()
            .sortedBy { dismissals[it] ?: 0 }

        return ranked.take(limit)
    }

    suspend fun dismiss(userId: String, itemName: String) = suggestionRepo.dismissSuggestion(userId, itemName)

    private fun currentMonthCode(): String {
        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val monthNumber = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber
        return months[monthNumber - 1]
    }
}

/** Detects frequently-bought items missing from any currently active list — feeds the nightly nudge worker (§2.8). */
class FindMissingFrequentItemsUseCase(
    private val suggestionRepo: SuggestionRepository,
    private val itemRepo: ShoppingItemRepository,
    private val listRepo: ShoppingListRepository
) {
    suspend operator fun invoke(userId: String, minCount: Int = 3): List<String> {
        val frequent = suggestionRepo.getFrequentItemNames(userId, minCount)
        if (frequent.isEmpty()) return emptyList()

        val activeItemNames = mutableSetOf<String>()
        listRepo.getAllListsOnce().forEach { list ->
            itemRepo.getItemsForListOnce(list.listId).forEach { activeItemNames.add(it.name.trim().lowercase()) }
        }
        return frequent.filter { it !in activeItemNames }
    }
}
