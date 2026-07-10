package com.shoppilist.shared.sync

import android.content.Context
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shoppilist.shared.data.local.AppDatabase
import com.shoppilist.shared.data.repository.RoomNotificationRepository
import com.shoppilist.shared.data.repository.RoomOfflineOpManager
import com.shoppilist.shared.data.repository.RoomShoppingItemRepository
import com.shoppilist.shared.data.repository.RoomShoppingListRepository
import com.shoppilist.shared.data.repository.RoomSuggestionRepository
import com.shoppilist.shared.domain.FindMissingFrequentItemsUseCase
import java.util.concurrent.TimeUnit

class AndroidProactiveSuggestionScheduler(private val context: Context) : ProactiveSuggestionScheduler {
    override fun schedule() {
        val request = PeriodicWorkRequestBuilder<ProactiveSuggestionWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        private const val WORK_NAME = "proactive_suggestions"
    }
}

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
                listRepo = RoomShoppingListRepository(db.shoppingListDao(), db.shoppingItemDao(), opManager)
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
}
