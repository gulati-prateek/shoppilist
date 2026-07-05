package com.shoppilist

import android.app.Application
import com.shoppilist.di.appModule
import com.shoppilist.shared.di.categoryModule
import com.shoppilist.shared.di.collaborationModule
import com.shoppilist.shared.di.notificationModule
import com.shoppilist.shared.di.repositoryModule
import com.shoppilist.shared.di.retailerModule
import com.shoppilist.shared.di.subListModule
import com.shoppilist.shared.di.suggestionModule
import com.shoppilist.shared.di.viewModelModule
import com.shoppilist.shared.sync.ProactiveSuggestionScheduler
import org.koin.android.ext.android.get
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
        get<ProactiveSuggestionScheduler>().schedule()
    }
}
