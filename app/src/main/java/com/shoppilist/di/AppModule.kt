package com.shoppilist.di

import android.content.Context
import androidx.room.Room
import com.russhwolf.settings.SharedPreferencesSettings
import com.shoppilist.auth.FirebaseAuthService
import com.shoppilist.backend.FirestoreBackend
import com.shoppilist.shared.auth.AuthService
import com.shoppilist.shared.backend.AdminBackend
import com.shoppilist.shared.backend.CatalogBackend
import com.shoppilist.shared.backend.ProfileBackend
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

    single<AuthService> { FirebaseAuthService() }

    // One Firestore client implements all three cloud interfaces.
    single { FirestoreBackend() }
    single<CatalogBackend> { get<FirestoreBackend>() }
    single<ProfileBackend> { get<FirestoreBackend>() }
    single<AdminBackend> { get<FirestoreBackend>() }
}
