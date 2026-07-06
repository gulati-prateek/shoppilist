package com.shoppilist.shared.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.russhwolf.settings.NSUserDefaultsSettings
import com.shoppilist.shared.data.local.AppDatabase
import com.shoppilist.shared.data.session.SessionManager
import com.shoppilist.shared.sync.IosProactiveSuggestionScheduler
import com.shoppilist.shared.sync.ProactiveSuggestionScheduler
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

/**
 * iOS counterpart to :app's appModule -- provides the same bindings (AppDatabase, SessionManager,
 * ProactiveSuggestionScheduler) using iOS-native construction instead of an Android Context.
 *
 * Known gap: unlike Android's appModule (which seeds category/grocery-app/retailer/global-item
 * data via a RoomDatabase.Callback on first creation), this does not seed the database yet --
 * Room's KMP-common Callback API takes a raw SQLiteConnection rather than the DAO-based access
 * DatabaseSeeder.seed() needs, so wiring that up is deferred rather than guessed at without a way
 * to verify it. Fine for proving the iOS entry point compiles and runs; needs revisiting before
 * the iOS app is functionally complete.
 */
@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}

val iosAppModule = module {
    single<AppDatabase> {
        Room.databaseBuilder<AppDatabase>(name = documentDirectory() + "/shoppilist.db")
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    single {
        SessionManager(NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults))
    }

    single<ProactiveSuggestionScheduler> { IosProactiveSuggestionScheduler() }
}
