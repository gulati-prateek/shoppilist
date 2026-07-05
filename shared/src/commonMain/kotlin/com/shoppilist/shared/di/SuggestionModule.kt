package com.shoppilist.shared.di

import com.shoppilist.shared.data.repository.RoomSuggestionRepository
import com.shoppilist.shared.data.repository.SuggestionRepository
import com.shoppilist.shared.domain.FindMissingFrequentItemsUseCase
import com.shoppilist.shared.domain.SuggestionEngine
import org.koin.dsl.module

val suggestionModule = module {
    single<SuggestionRepository> { RoomSuggestionRepository(get(), get(), get()) }
    single { SuggestionEngine(get()) }
    factory { FindMissingFrequentItemsUseCase(get(), get(), get()) }
}
