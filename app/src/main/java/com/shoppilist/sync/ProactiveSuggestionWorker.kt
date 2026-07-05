package com.shoppilist.sync

import android.content.Context
import androidx.room.Room
import androidx.work.*
import com.shoppilist.shared.data.local.AppDatabase
import com.shoppilist.shared.data.repository.RoomNotificationRepository
import com.shoppilist.shared.data.repository.RoomOfflineOpManager
import com.shoppilist.shared.data.repository.RoomShoppingItemRepository
import com.shoppilist.shared.data.repository.RoomShoppingListRepository
import com.shoppilist.shared.data.repository.RoomSuggestionRepository
import com.shoppilist.shared.domain.FindMissingFrequentItemsUseCase
import java.util.concurrent.TimeUnit

/**
 * Nightly nudge (§2.8): "You usually add Eggs on Sundays. Add to Weekly list?" — finds items
 * the user frequently buys that aren't in any of their active lists right now.
 */
class ProactiveSuggestionWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shoppilist.db").build()
        try {
            val userId = applicationContext
                .getSharedPreferences("shoppilist_session", Context.MODE_PRIVATE)
                .getString("current_user_id", null) ?: return Result.success()

            val opManager = RoomOfflineOpManager(db.pendingOpDao())
            val useCase = FindMissingFrequentItemsUseCase(
                suggestionRepo = RoomSuggestionRepository(db.itemHistoryDao(), db.suggestionDismissalDao(), db.globalItemDao()),
                itemRepo = RoomShoppingItemRepository(db.shoppingItemDao(), opManager),
                listRepo = RoomShoppingListRepository(db.shoppingListDao(), opManager)
            )

            val missing = useCase(userId)
            if (missing.isNotEmpty()) {
                RoomNotificationRepository(db.notificationDao()).send(
                    userId = userId,
                    title = "Running low on something?",
                    body = "You usually buy ${missing.take(3).joinToString(", ")}. Add to a list?"
                )
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        } finally {
            db.close()
        }
    }

    companion object {
        private const val WORK_NAME = "proactive_suggestions"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ProactiveSuggestionWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
