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
    val db = koinApp.koin.get<AppDatabase>()
    CoroutineScope(Dispatchers.Default).launch {
        if (db.itemCategoryDao().count() == 0) {
            DatabaseSeeder.seed(db)
        }
    }
}
