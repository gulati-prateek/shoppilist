package com.shoppilist

import android.app.Application
import com.shoppilist.di.appModule
import com.shoppilist.di.categoryModule
import com.shoppilist.di.collaborationModule
import com.shoppilist.di.notificationModule
import com.shoppilist.di.repositoryModule
import com.shoppilist.di.retailerModule
import com.shoppilist.di.subListModule
import com.shoppilist.di.suggestionModule
import com.shoppilist.di.viewModelModule
import com.shoppilist.sync.ProactiveSuggestionWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ShoppiListApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ShoppiListApp)
            modules(
                appModule,
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
        ProactiveSuggestionWorker.schedule(this)
    }
}
