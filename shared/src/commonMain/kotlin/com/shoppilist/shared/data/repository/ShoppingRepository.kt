@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.data.repository

import com.shoppilist.shared.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

interface ShoppingListRepository {
    fun getAllLists(): Flow<List<ShoppingListEntity>>
    suspend fun getAllListsOnce(): List<ShoppingListEntity>
    fun getListFlow(listId: String): Flow<ShoppingListEntity?>
    fun getSubLists(parentListId: String): Flow<List<ShoppingListEntity>>
    suspend fun getListOnce(listId: String): ShoppingListEntity?
    suspend fun createList(list: ShoppingListEntity): Result<String>
    suspend fun updateList(list: ShoppingListEntity): Result<Unit>
    suspend fun deleteList(listId: String): Result<Unit>
    suspend fun archiveList(listId: String): Result<Unit>
    suspend fun setPinned(listId: String, pinned: Boolean): Result<Unit>
    suspend fun setParent(listId: String, parentListId: String?): Result<Unit>
}

interface ShoppingItemRepository {
    fun getItemsForList(listId: String): Flow<List<ShoppingItemEntity>>
    fun getItemsAssignedTo(listId: String, userId: String): Flow<List<ShoppingItemEntity>>
    fun getItemFlow(itemId: String): Flow<ShoppingItemEntity?>
    suspend fun getItemOnce(itemId: String): ShoppingItemEntity?
    suspend fun getUncheckedItemsOnce(listId: String): List<ShoppingItemEntity>
    suspend fun getItemsForListOnce(listId: String): List<ShoppingItemEntity>
    suspend fun getItemsByIds(itemIds: List<String>): List<ShoppingItemEntity>
    suspend fun addItem(item: ShoppingItemEntity): Result<String>
    suspend fun updateItem(item: ShoppingItemEntity): Result<Unit>
    suspend fun deleteItem(itemId: String): Result<Unit>
    suspend fun markItemChecked(itemId: String, checked: Boolean): Result<Unit>
    suspend fun clearPurchased(listId: String): Result<Unit>
    suspend fun assignItem(itemId: String, userId: String, assignedBy: String): Result<Unit>
    suspend fun unassignItem(itemId: String): Result<Unit>
    suspend fun unassignAllForUserInList(listId: String, userId: String): Result<Unit>
    suspend fun updateCategory(itemId: String, categoryId: String?, overriddenBy: String?): Result<Unit>
    suspend fun updateSortOrder(itemId: String, sortOrder: Int): Result<Unit>
    suspend fun moveItemsToList(itemIds: List<String>, newListId: String): Result<Unit>
}

interface OfflineOpManager {
    fun getPendingOps(): Flow<List<PendingOpEntity>>
    suspend fun queueOp(op: PendingOpEntity)
    suspend fun markOpSynced(opId: String)
    suspend fun markOpFailed(opId: String)
    suspend fun clearOp(opId: String)
}

class RoomShoppingListRepository(
    private val listDao: ShoppingListDao,
    private val opManager: OfflineOpManager
) : ShoppingListRepository {

    override fun getAllLists(): Flow<List<ShoppingListEntity>> = listDao.getAllLists()

    override suspend fun getAllListsOnce(): List<ShoppingListEntity> = listDao.getAllListsOnce()

    override fun getListFlow(listId: String): Flow<ShoppingListEntity?> = listDao.getListFlow(listId)

    override fun getSubLists(parentListId: String): Flow<List<ShoppingListEntity>> = listDao.getSubLists(parentListId)

    override suspend fun getListOnce(listId: String): ShoppingListEntity? = listDao.getListOnce(listId)

    override suspend fun setPinned(listId: String, pinned: Boolean): Result<Unit> {
        return try {
            listDao.setPinned(listId, pinned)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setParent(listId: String, parentListId: String?): Result<Unit> {
        return try {
            listDao.setParent(listId, parentListId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createList(list: ShoppingListEntity): Result<String> {
        return try {
            listDao.upsert(list)
            // Queue offline op with serialized JSON payload
            val payload = """{"listId":"${list.listId}","name":"${list.name}","description":"${list.description ?: ""}"}"""
            opManager.queueOp(
                PendingOpEntity(
                    opId = Uuid.random().toString(),
                    opType = "CREATE_LIST",
                    targetId = list.listId,
                    payload = payload,
                    status = "PENDING"
                )
            )
            Result.success(list.listId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateList(list: ShoppingListEntity): Result<Unit> {
        return try {
            listDao.upsert(list)
            val payload = """{"listId":"${list.listId}","name":"${list.name}"}"""
            opManager.queueOp(
                PendingOpEntity(
                    opId = Uuid.random().toString(),
                    opType = "UPDATE_LIST",
                    targetId = list.listId,
                    payload = payload,
                    status = "PENDING"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteList(listId: String): Result<Unit> {
        return try {
            listDao.delete(listId)
            opManager.queueOp(
                PendingOpEntity(
                    opId = Uuid.random().toString(),
                    opType = "DELETE_LIST",
                    targetId = listId,
                    payload = """{"listId":"$listId"}""",
                    status = "PENDING"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun archiveList(listId: String): Result<Unit> {
        return try {
            listDao.archive(listId)
            opManager.queueOp(
                PendingOpEntity(
                    opId = Uuid.random().toString(),
                    opType = "ARCHIVE_LIST",
                    targetId = listId,
                    payload = """{"listId":"$listId"}""",
                    status = "PENDING"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RoomShoppingItemRepository(
    private val itemDao: ShoppingItemDao,
    private val opManager: OfflineOpManager
) : ShoppingItemRepository {

    override fun getItemsForList(listId: String): Flow<List<ShoppingItemEntity>> = itemDao.getItemsForList(listId)

    override fun getItemsAssignedTo(listId: String, userId: String): Flow<List<ShoppingItemEntity>> =
        itemDao.getItemsAssignedTo(listId, userId)

    override fun getItemFlow(itemId: String): Flow<ShoppingItemEntity?> = itemDao.getItemFlow(itemId)

    override suspend fun getItemOnce(itemId: String): ShoppingItemEntity? = itemDao.getItemOnce(itemId)

    override suspend fun getUncheckedItemsOnce(listId: String): List<ShoppingItemEntity> =
        itemDao.getUncheckedItemsOnce(listId)

    override suspend fun getItemsForListOnce(listId: String): List<ShoppingItemEntity> =
        itemDao.getItemsForListOnce(listId)

    override suspend fun getItemsByIds(itemIds: List<String>): List<ShoppingItemEntity> =
        itemDao.getItemsByIds(itemIds)

    override suspend fun clearPurchased(listId: String): Result<Unit> {
        return try {
            itemDao.clearPurchased(listId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun assignItem(itemId: String, userId: String, assignedBy: String): Result<Unit> {
        return try {
            itemDao.assignItem(itemId, userId, assignedBy, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unassignItem(itemId: String): Result<Unit> {
        return try {
            itemDao.unassignItem(itemId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unassignAllForUserInList(listId: String, userId: String): Result<Unit> {
        return try {
            itemDao.unassignAllForUserInList(listId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(itemId: String, categoryId: String?, overriddenBy: String?): Result<Unit> {
        return try {
            itemDao.updateCategory(itemId, categoryId, overriddenBy)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSortOrder(itemId: String, sortOrder: Int): Result<Unit> {
        return try {
            itemDao.updateSortOrder(itemId, sortOrder)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveItemsToList(itemIds: List<String>, newListId: String): Result<Unit> {
        return try {
            itemDao.moveItemsToList(itemIds, newListId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addItem(item: ShoppingItemEntity): Result<String> {
        return try {
            itemDao.upsert(item)
            val payload = """{"itemId":"${item.itemId}","listId":"${item.listId}","name":"${item.name}","quantity":${item.quantity}}"""
            opManager.queueOp(
                PendingOpEntity(
                    opId = Uuid.random().toString(),
                    opType = "ADD_ITEM",
                    targetId = item.itemId,
                    payload = payload,
                    status = "PENDING"
                )
            )
            Result.success(item.itemId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateItem(item: ShoppingItemEntity): Result<Unit> {
        return try {
            itemDao.upsert(item)
            val payload = """{"itemId":"${item.itemId}","name":"${item.name}","quantity":${item.quantity}}"""
            opManager.queueOp(
                PendingOpEntity(
                    opId = Uuid.random().toString(),
                    opType = "UPDATE_ITEM",
                    targetId = item.itemId,
                    payload = payload,
                    status = "PENDING"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteItem(itemId: String): Result<Unit> {
        return try {
            itemDao.deleteItem(itemId)
            opManager.queueOp(
                PendingOpEntity(
                    opId = Uuid.random().toString(),
                    opType = "DELETE_ITEM",
                    targetId = itemId,
                    payload = """{"itemId":"$itemId"}""",
                    status = "PENDING"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markItemChecked(itemId: String, checked: Boolean): Result<Unit> {
        return try {
            itemDao.setChecked(itemId, checked)
            opManager.queueOp(
                PendingOpEntity(
                    opId = Uuid.random().toString(),
                    opType = "MARK_ITEM_CHECKED",
                    targetId = itemId,
                    payload = """{"itemId":"$itemId","checked":$checked}""",
                    status = "PENDING"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RoomOfflineOpManager(private val pendingOpDao: PendingOpDao) : OfflineOpManager {
    override fun getPendingOps(): Flow<List<PendingOpEntity>> = pendingOpDao.getPendingOps()

    override suspend fun queueOp(op: PendingOpEntity) = pendingOpDao.insert(op)

    override suspend fun markOpSynced(opId: String) = pendingOpDao.updateStatus(opId, "SYNCED")

    override suspend fun markOpFailed(opId: String) = pendingOpDao.updateStatus(opId, "FAILED")

    override suspend fun clearOp(opId: String) = pendingOpDao.delete(opId)
}


