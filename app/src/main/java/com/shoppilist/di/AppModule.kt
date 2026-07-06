package com.shoppilist.di

import android.content.Context
import androidx.room.Room
import com.russhwolf.settings.SharedPreferencesSettings
import com.shoppilist.shared.data.local.AppDatabase
import com.shoppilist.data.local.seed.DatabaseSeederCallback
import com.shoppilist.shared.data.session.SessionManager
import com.shoppilist.shared.sync.AndroidProactiveSuggestionScheduler
import com.shoppilist.shared.sync.ProactiveSuggestionScheduler
import org.koin.dsl.module

val appModule = module {

    single {
        lateinit var instance: AppDatabase
        instance = Room.databaseBuilder(get(), AppDatabase::class.java, "shoppilist.db")
            .fallbackToDestructiveMigration()
            .addCallback(DatabaseSeederCallback.callback { instance })
            .build()
        instance
    }

    single {
        val prefs = get<Context>().getSharedPreferences("shoppilist_session", Context.MODE_PRIVATE)
        SessionManager(SharedPreferencesSettings(prefs))
    }

    single<ProactiveSuggestionScheduler> { AndroidProactiveSuggestionScheduler(get()) }
}
