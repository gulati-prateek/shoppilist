package com.shoppilist.shared.di

import com.shoppilist.shared.data.repository.ItemCategoryRepository
import com.shoppilist.shared.data.repository.RoomItemCategoryRepository
import com.shoppilist.shared.domain.*
import org.koin.dsl.module

val categoryModule = module {
    single<ItemCategoryRepository> { RoomItemCategoryRepository(get(), get(), get()) }
    single { CategoryMatcher(get()) }
    factory { GetCategoriesForListUseCase(get()) }
    factory { AutoCategorizeItemUseCase(get()) }
    factory { OverrideItemCategoryUseCase(get(), get()) }
    factory { RenameCategoryForListUseCase(get()) }
    factory { CreateCustomCategoryUseCase(get()) }
}
