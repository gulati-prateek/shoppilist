@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.domain

import com.shoppilist.shared.data.local.ListActivityAction
import com.shoppilist.shared.data.local.ListActivityDao
import com.shoppilist.shared.data.local.ListActivityEntity
import com.shoppilist.shared.data.local.UserDao
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

/**
 * Appends an entry to a list's activity feed (item 11). The actor's display name is resolved and
 * denormalized at write time so the feed renders without a user join, and so it stays correct even
 * if the actor later leaves the list. Best-effort: recording activity must never fail the primary
 * action (add/check/rename), so callers invoke this fire-and-forget.
 */
class RecordActivityUseCase(
    private val listActivityDao: ListActivityDao,
    private val userDao: UserDao
) {
    suspend operator fun invoke(
        listId: String,
        actorUserId: String,
        action: ListActivityAction,
        itemName: String? = null,
        detail: String? = null
    ) {
        val actorName = userDao.getUserOnce(actorUserId)?.fullName?.takeIf { it.isNotBlank() } ?: "Someone"
        listActivityDao.insert(
            ListActivityEntity(
                id = Uuid.random().toString(),
                listId = listId,
                actorUserId = actorUserId,
                actorName = actorName,
                action = action,
                itemName = itemName,
                detail = detail
            )
        )
    }
}

class GetListActivityUseCase(private val listActivityDao: ListActivityDao) {
    operator fun invoke(listId: String): Flow<List<ListActivityEntity>> = listActivityDao.getForList(listId)
}
