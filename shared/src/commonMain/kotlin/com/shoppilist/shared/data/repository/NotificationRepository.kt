@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.data.repository

import com.shoppilist.shared.data.local.NotificationDao
import com.shoppilist.shared.data.local.NotificationEntity
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface NotificationRepository {
    fun getNotifications(userId: String): Flow<List<NotificationEntity>>
    suspend fun send(userId: String, title: String, body: String, dataPayload: String? = null): Result<Unit>
}

class RoomNotificationRepository(private val dao: NotificationDao) : NotificationRepository {
    override fun getNotifications(userId: String): Flow<List<NotificationEntity>> = dao.getNotifications(userId)

    override suspend fun send(userId: String, title: String, body: String, dataPayload: String?): Result<Unit> {
        return try {
            dao.upsert(
                NotificationEntity(
                    notificationId = Uuid.random().toString(),
                    userId = userId,
                    title = title,
                    body = body,
                    dataPayload = dataPayload
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
