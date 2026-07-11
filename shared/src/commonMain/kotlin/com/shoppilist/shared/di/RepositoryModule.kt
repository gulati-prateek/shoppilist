package com.shoppilist.shared.di

import com.shoppilist.shared.data.repository.*
import com.shoppilist.shared.domain.*
import org.koin.dsl.module

val repositoryModule = module {

    single<OfflineOpManager> { RoomOfflineOpManager(get()) }
    single<ShoppingListRepository> { RoomShoppingListRepository(get(), get(), get()) }
    single<ShoppingItemRepository> { RoomShoppingItemRepository(get(), get()) }

    // Account mirror + region catalog sync
    single { UserAccountSync(get(), get(), get()) }
    factory { ResolveCatalogRegionUseCase(get(), get()) }
    factory { SyncCatalogUseCase(get(), get(), get()) }

    // UseCases
    factory { GetAllListsUseCase(get()) }
    factory { GetListUseCase(get()) }
    factory { CreateListUseCase(get()) }
    factory { DeleteListUseCase(get()) }
    factory { ArchiveListUseCase(get()) }
    factory { GetArchivedListsUseCase(get()) }
    factory { UnarchiveListUseCase(get()) }
    factory { TogglePinUseCase(get()) }
    factory { RenameListUseCase(get()) }
    factory { GetListItemsUseCase(get()) }
    factory { GetItemOnceUseCase(get()) }
    factory { AddItemUseCase(get(), get(), get(), get(), get()) }
    factory { UpdateItemUseCase(get()) }
    factory { DeleteItemUseCase(get()) }
    factory { MarkItemCheckedUseCase(get()) }
    factory { ClearPurchasedUseCase(get()) }

    // Assignment (§2.11)
    factory { AssignItemUseCase(get(), get()) }
    factory { UnassignItemUseCase(get()) }
    factory { GetMyItemsUseCase(get()) }
    factory { GetAssignmentSummaryUseCase() }
}
