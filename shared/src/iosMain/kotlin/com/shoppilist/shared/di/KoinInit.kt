package com.shoppilist.shared.di

import org.koin.core.context.startKoin

fun initKoinIos() {
    startKoin {
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
}
