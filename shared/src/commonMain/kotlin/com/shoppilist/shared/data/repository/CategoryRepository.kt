@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.data.repository

import com.shoppilist.shared.data.local.CategoryCorrectionDao
import com.shoppilist.shared.data.local.CategoryCorrectionEntity
import com.shoppilist.shared.data.local.GlobalItemDao
import com.shoppilist.shared.data.local.GlobalItemEntity
import com.shoppilist.shared.data.local.ItemCategoryDao
import com.shoppilist.shared.data.local.ItemCategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface ItemCategoryRepository {
    fun getCategoriesForList(listId: String): Flow<List<ItemCategoryEntity>>
    fun getGlobalCategories(): Flow<List<ItemCategoryEntity>>
    suspend fun getById(id: String): ItemCategoryEntity?
    suspend fun findGlobalItemByName(name: String): GlobalItemEntity?
    suspend fun searchGlobalItemsByPrefix(prefix: String, limit: Int = 5): List<GlobalItemEntity>
    suspend fun getLatestCorrection(itemName: String): CategoryCorrectionEntity?
    suspend fun recordCorrection(itemName: String, suggestedCategoryId: String?, correctCategoryId: String, correctedBy: String)
    suspend fun renameCategoryForList(listId: String, sourceCategoryId: String, newName: String): Result<String>
    suspend fun createCustomCategory(listId: String, name: String, emoji: String, displayOrder: Int): Result<String>
}

class RoomItemCategoryRepository(
    private val categoryDao: ItemCategoryDao,
    private val correctionDao: CategoryCorrectionDao,
    private val globalItemDao: GlobalItemDao
) : ItemCategoryRepository {

    override fun getCategoriesForList(listId: String): Flow<List<ItemCategoryEntity>> =
        categoryDao.getCategoriesForList(listId)

    override fun getGlobalCategories(): Flow<List<ItemCategoryEntity>> = categoryDao.getGlobalCategories()

    override suspend fun getById(id: String): ItemCategoryEntity? = categoryDao.getById(id)

    override suspend fun findGlobalItemByName(name: String): GlobalItemEntity? =
        globalItemDao.findByName(name.trim().lowercase())

    override suspend fun searchGlobalItemsByPrefix(prefix: String, limit: Int): List<GlobalItemEntity> =
        globalItemDao.searchByPrefix(prefix.trim().lowercase(), limit)

    override suspend fun getLatestCorrection(itemName: String): CategoryCorrectionEntity? =
        correctionDao.getLatestCorrection(itemName.trim().lowercase())

    override suspend fun recordCorrection(
        itemName: String,
        suggestedCategoryId: String?,
        correctCategoryId: String,
        correctedBy: String
    ) {
        correctionDao.insert(
            CategoryCorrectionEntity(
                id = Uuid.random().toString(),
                itemName = itemName.trim().lowercase(),
                suggestedCategoryId = suggestedCategoryId,
                correctCategoryId = correctCategoryId,
                correctedBy = correctedBy
            )
        )
    }

    override suspend fun renameCategoryForList(listId: String, sourceCategoryId: String, newName: String): Result<String> {
        return try {
            val source = categoryDao.getById(sourceCategoryId) ?: return Result.failure(IllegalArgumentException("Category not found"))
            val newId = "${sourceCategoryId}_list_$listId"
            categoryDao.upsert(source.copy(categoryId = newId, name = newName, listId = listId))
            Result.success(newId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createCustomCategory(listId: String, name: String, emoji: String, displayOrder: Int): Result<String> {
        return try {
            val id = "custom_${Uuid.random()}"
            categoryDao.upsert(ItemCategoryEntity(id, name, emoji, displayOrder, countryCode = null, listId = listId))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
