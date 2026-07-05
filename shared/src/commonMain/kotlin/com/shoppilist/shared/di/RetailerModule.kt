package com.shoppilist.shared.di

import com.shoppilist.shared.data.repository.GroceryAppRepository
import com.shoppilist.shared.data.repository.RoomGroceryAppRepository
import com.shoppilist.shared.data.repository.RoomSponsoredRetailerRepository
import com.shoppilist.shared.data.repository.SponsoredRetailerRepository
import com.shoppilist.shared.domain.*
import org.koin.dsl.module

val retailerModule = module {
    single<SponsoredRetailerRepository> { RoomSponsoredRetailerRepository(get(), get()) }
    single<GroceryAppRepository> { RoomGroceryAppRepository(get()) }

    factory { GetRetailersForCountryUseCase(get()) }
    factory { GetOrderOnlineOptionsUseCase(get()) }
    factory { LogSponsoredClickUseCase(get()) }
    factory { GetGroceryAppsForCountryUseCase(get()) }
}
