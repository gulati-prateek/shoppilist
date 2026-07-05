package com.shoppilist.di

import com.shoppilist.shared.domain.*
import org.koin.dsl.module

val subListModule = module {
    factory { CreateSubListUseCase(get(), get()) }
    factory { PromoteSubListUseCase(get()) }
    factory { DoneShoppingUseCase(get()) }
    factory { CreateLeftoverListUseCase(get(), get()) }
    factory { MergeListsUseCase(get(), get()) }
}
