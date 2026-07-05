package com.shoppilist.di

import android.content.Context
import androidx.room.Room
import com.russhwolf.settings.SharedPreferencesSettings
import com.shoppilist.shared.data.local.*
import com.shoppilist.data.local.seed.DatabaseSeederCallback
import com.shoppilist.shared.data.session.SessionManager
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

    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().householdDao() }
    single { get<AppDatabase>().householdMemberDao() }
    single { get<AppDatabase>().listMemberDao() }
    single { get<AppDatabase>().shoppingListDao() }
    single { get<AppDatabase>().shoppingItemDao() }
    single { get<AppDatabase>().itemCategoryDao() }
    single { get<AppDatabase>().categoryCorrectionDao() }
    single { get<AppDatabase>().globalItemDao() }
    single { get<AppDatabase>().itemHistoryDao() }
    single { get<AppDatabase>().suggestionDismissalDao() }
    single { get<AppDatabase>().groceryAppDao() }
    single { get<AppDatabase>().sponsoredRetailerDao() }
    single { get<AppDatabase>().sponsoredClickDao() }
    single { get<AppDatabase>().presenceDao() }
    single { get<AppDatabase>().invitationDao() }
    single { get<AppDatabase>().notificationDao() }
    single { get<AppDatabase>().voiceDao() }
    single { get<AppDatabase>().affiliateDao() }
    single { get<AppDatabase>().pendingOpDao() }

    single {
        val prefs = get<Context>().getSharedPreferences("shoppilist_session", Context.MODE_PRIVATE)
        SessionManager(SharedPreferencesSettings(prefs))
    }
}
