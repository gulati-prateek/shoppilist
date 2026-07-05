package com.shoppilist.shared.di

import com.shoppilist.shared.data.repository.*
import com.shoppilist.shared.data.session.SessionManager
import com.shoppilist.shared.domain.*
import com.shoppilist.shared.voice.CommandExecutor
import com.shoppilist.shared.voice.RuleBasedProcessor
import com.shoppilist.shared.voice.VoiceIntentProcessor
import org.koin.dsl.module

val repositoryModule = module {

    single<OfflineOpManager> { RoomOfflineOpManager(get()) }
    single<ShoppingListRepository> { RoomShoppingListRepository(get(), get()) }
    single<ShoppingItemRepository> { RoomShoppingItemRepository(get(), get()) }

    // UseCases
    factory { GetAllListsUseCase(get()) }
    factory { GetListUseCase(get()) }
    factory { CreateListUseCase(get()) }
    factory { DeleteListUseCase(get()) }
    factory { ArchiveListUseCase(get()) }
    factory { TogglePinUseCase(get()) }
    factory { GetListItemsUseCase(get()) }
    factory { GetItemOnceUseCase(get()) }
    factory { AddItemUseCase(get(), get(), get()) }
    factory { UpdateItemUseCase(get()) }
    factory { DeleteItemUseCase(get()) }
    factory { MarkItemCheckedUseCase(get()) }
    factory { ClearPurchasedUseCase(get()) }

    // Assignment (§2.11)
    factory { AssignItemUseCase(get(), get()) }
    factory { UnassignItemUseCase(get()) }
    factory { GetMyItemsUseCase(get()) }
    factory { GetAssignmentSummaryUseCase() }

    // Voice
    single<VoiceIntentProcessor> { RuleBasedProcessor() }
    factory {
        CommandExecutor(
            get<GetAllListsUseCase>(),
            get<GetListUseCase>(),
            get<CreateListUseCase>(),
            get<AddItemUseCase>(),
            get<DeleteItemUseCase>(),
            get<MarkItemCheckedUseCase>(),
            get<GetListItemsUseCase>(),
            get<DeleteListUseCase>(),
            get<SessionManager>().requireUserId()
        )
    }
}
