package com.shoppilist.data.local.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shoppilist.shared.data.local.AppDatabase
import com.shoppilist.shared.data.local.seed.DatabaseSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android-only bridge into [DatabaseSeeder]: `RoomDatabase.Callback`/`SupportSQLiteDatabase` are
 * classic Android Room APIs with no Kotlin/Native equivalent, so this stays in `:app` while the
 * actual seeding logic lives in `:shared`.
 */
object DatabaseSeederCallback {
    fun callback(databaseProvider: () -> AppDatabase): RoomDatabase.Callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                DatabaseSeeder.seed(databaseProvider())
            }
        }
    }
}
