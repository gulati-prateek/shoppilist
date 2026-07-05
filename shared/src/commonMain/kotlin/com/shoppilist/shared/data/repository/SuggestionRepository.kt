@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.data.repository

import com.shoppilist.shared.data.local.GlobalItemDao
import com.shoppilist.shared.data.local.GlobalItemEntity
import com.shoppilist.shared.data.local.ItemHistoryDao
import com.shoppilist.shared.data.local.ItemHistoryEntity
import com.shoppilist.shared.data.local.ItemNameCount
import com.shoppilist.shared.data.local.SuggestionDismissalDao
import com.shoppilist.shared.data.local.SuggestionDismissalEntity
import kotlin.uuid.Uuid

interface SuggestionRepository {
    suspend fun recordItemAdded(userId: String, itemName: String, listId: String)
    suspend fun getTopHistoryItems(userId: String, limit: Int = 20): List<ItemNameCount>
    suspend fun searchHistory(userId: String, prefix: String, limit: Int = 5): List<ItemNameCount>
    suspend fun getFrequentItemNames(userId: String, minCount: Int = 3): List<String>
    suspend fun dismissSuggestion(userId: String, itemName: String)
    suspend fun getDismissalCounts(userId: String): Map<String, Int>
    suspend fun searchGlobalItems(prefix: String, limit: Int = 20): List<GlobalItemEntity>
    suspend fun getSeasonalItems(): List<GlobalItemEntity>
}

class RoomSuggestionRepository(
    private val historyDao: ItemHistoryDao,
    private val dismissalDao: SuggestionDismissalDao,
    private val globalItemDao: GlobalItemDao
) : SuggestionRepository {

    override suspend fun recordItemAdded(userId: String, itemName: String, listId: String) {
        val normalized = itemName.trim().lowercase()
        val existing = historyDao.find(userId, normalized, listId)
        historyDao.upsert(
            ItemHistoryEntity(
                id = existing?.id ?: Uuid.random().toString(),
                userId = userId,
                itemName = normalized,
                listId = listId,
                addedCount = (existing?.addedCount ?: 0) + 1,
                lastAddedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getTopHistoryItems(userId: String, limit: Int): List<ItemNameCount> =
        historyDao.getTopItemNames(userId, limit)

    override suspend fun searchHistory(userId: String, prefix: String, limit: Int): List<ItemNameCount> =
        historyDao.searchByPrefix(userId, prefix.trim().lowercase(), limit)

    override suspend fun getFrequentItemNames(userId: String, minCount: Int): List<String> =
        historyDao.getFrequentItemNames(userId, minCount)

    override suspend fun dismissSuggestion(userId: String, itemName: String) {
        val normalized = itemName.trim().lowercase()
        val existing = dismissalDao.find(userId, normalized)
        dismissalDao.upsert(
            SuggestionDismissalEntity(
                id = existing?.id ?: Uuid.random().toString(),
                userId = userId,
                itemName = normalized,
                dismissCount = (existing?.dismissCount ?: 0) + 1,
                lastDismissedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getDismissalCounts(userId: String): Map<String, Int> =
        dismissalDao.getForUser(userId).associate { it.itemName to it.dismissCount }

    override suspend fun searchGlobalItems(prefix: String, limit: Int): List<GlobalItemEntity> =
        globalItemDao.searchByPrefix(prefix.trim().lowercase(), limit)

    override suspend fun getSeasonalItems(): List<GlobalItemEntity> = globalItemDao.getSeasonalItems()
}
