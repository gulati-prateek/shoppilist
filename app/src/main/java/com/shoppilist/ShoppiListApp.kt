package com.shoppilist

import android.app.Application
import com.shoppilist.di.appModule
import com.shoppilist.shared.data.local.AppDatabase
import com.shoppilist.shared.data.local.seed.DatabaseSeeder
import com.shoppilist.shared.di.categoryModule
import com.shoppilist.shared.di.collaborationModule
import com.shoppilist.shared.di.databaseDaoModule
import com.shoppilist.shared.di.notificationModule
import com.shoppilist.shared.di.repositoryModule
import com.shoppilist.shared.di.retailerModule
import com.shoppilist.shared.di.subListModule
import com.shoppilist.shared.di.suggestionModule
import com.shoppilist.shared.di.viewModelModule
import com.shoppilist.shared.sync.ProactiveSuggestionScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ShoppiListApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ShoppiListApp)
            modules(
                appModule,
                databaseDaoModule,
                categoryModule,
                collaborationModule,
                notificationModule,
                repositoryModule,
                retailerModule,
                subListModule,
                suggestionModule,
                viewModelModule
            )
        }
        get<ProactiveSuggestionScheduler>().schedule()
        seedCatalogIfEmpty()
    }

    /**
     * The catalog is normally seeded from Room's onCreate callback (see appModule), but that only
     * fires on first DB creation — an in-place upgrade that keeps the DB file would leave the
     * create-list picker empty. This guarantees the catalog is present regardless (mirrors the iOS
     * seed-if-empty in KoinInit). Best-effort, off the main thread, never blocks startup.
     */
    private fun seedCatalogIfEmpty() {
        val db = get<AppDatabase>()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (db.itemCategoryDao().count() == 0) DatabaseSeeder.seed(db)
            } catch (_: Throwable) {
                // Seeding is best-effort; a failure must not crash launch.
            }
        }
    }
}
