package com.shoppilist.shared.domain

import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.data.repository.NotificationRepository
import com.shoppilist.shared.data.repository.ShoppingItemRepository

class AssignItemUseCase(
    private val repo: ShoppingItemRepository,
    private val notificationRepo: NotificationRepository
) {
    suspend operator fun invoke(itemId: String, itemName: String, listName: String, userId: String, assignedBy: String, assignedByName: String): Result<Unit> {
        val result = repo.assignItem(itemId, userId, assignedBy)
        if (result.isSuccess && userId != assignedBy) {
            notificationRepo.send(
                userId = userId,
                title = "New item assigned",
                body = "$assignedByName assigned you \"$itemName\" in $listName"
            )
        }
        return result
    }
}

class UnassignItemUseCase(private val repo: ShoppingItemRepository) {
    suspend operator fun invoke(itemId: String): Result<Unit> = repo.unassignItem(itemId)
}

class GetMyItemsUseCase(private val repo: ShoppingItemRepository) {
    operator fun invoke(listId: String, userId: String) = repo.getItemsAssignedTo(listId, userId)
}

data class AssigneeSummary(
    val userId: String,
    val items: List<ShoppingItemEntity>,
    val checkedCount: Int
)

/** Powers the "Who's Getting What" panel (§2.11): items grouped by assignee, plus the unassigned bucket. */
class GetAssignmentSummaryUseCase {
    operator fun invoke(items: List<ShoppingItemEntity>): Pair<List<AssigneeSummary>, List<ShoppingItemEntity>> {
        val unassigned = items.filter { it.assignedTo == null }
        val byAssignee = items
            .filter { it.assignedTo != null }
            .groupBy { it.assignedTo!! }
            .map { (userId, assignedItems) ->
                AssigneeSummary(userId, assignedItems, assignedItems.count { it.checked })
            }
        return byAssignee to unassigned
    }
}
