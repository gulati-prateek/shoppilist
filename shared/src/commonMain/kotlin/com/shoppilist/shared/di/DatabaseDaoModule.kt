package com.shoppilist.shared.di

import com.shoppilist.shared.data.local.AppDatabase
import org.koin.dsl.module

/**
 * DAO bindings only depend on [AppDatabase], which is already common -- portable across platforms.
 * Constructing the [AppDatabase] instance itself (platform-specific builder args) stays in each
 * platform's own module (:app's appModule for Android, iosAppModule for iOS).
 */
val databaseDaoModule = module {
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
}
