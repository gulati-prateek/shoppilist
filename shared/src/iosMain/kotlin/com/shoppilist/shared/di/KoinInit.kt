package com.shoppilist.shared.di

import com.shoppilist.shared.data.local.AppDatabase
import com.shoppilist.shared.data.local.seed.DatabaseSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin

fun initKoinIos() {
    val koinApp = startKoin {
        modules(
            iosAppModule,
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

    // Android seeds via a RoomDatabase.Callback.onCreate() hook (needs SupportSQLiteDatabase,
    // no Kotlin/Native equivalent); this is the iOS substitute -- a plain first-launch check
    // against a DAO that already exists everywhere, run once right after Koin resolves the DB.
    // A bare CoroutineScope(...).launch has no exception handler, so any failure here (a
    // seeding bug, a locked DB file, anything) would otherwise crash the whole app at launch --
    // an empty/partial catalog is recoverable, a crashed launch is not, so this must not throw.
    CoroutineScope(Dispatchers.Default).launch {
        try {
            val db = koinApp.koin.get<AppDatabase>()
            if (db.itemCategoryDao().count() == 0) {
                DatabaseSeeder.seed(db)
            }
        } catch (e: Throwable) {
            // Best-effort seeding; swallow and let the app continue with whatever landed.
        }
    }
}
