package com.shoppilist.shared.di

import com.shoppilist.shared.data.repository.*
import com.shoppilist.shared.domain.*
import org.koin.dsl.module

val collaborationModule = module {
    single<ListMemberRepository> { RoomListMemberRepository(get(), get()) }
    single<PresenceRepository> { RoomPresenceRepository(get()) }
    single<InvitationRepository> { RoomInvitationRepository(get(), get()) }

    // Phase 4 cross-device sync (backend bound per-platform in the app/iOS modules).
    single {
        com.shoppilist.shared.sync.CollaborationSyncManager(get(), get(), get(), get(), get(), get())
    }

    factory { GetListMembersUseCase(get()) }
    factory { AddListMemberUseCase(get()) }
    factory { RemoveListMemberUseCase(get()) }
    factory { CreateInviteUseCase(get()) }
    factory { GetInvitesForListUseCase(get()) }
    factory { GetPendingInvitesForContactUseCase(get()) }
    factory { AcceptInviteUseCase(get()) }
    factory { DeclineInviteUseCase(get()) }
    factory { MarkPresenceUseCase(get()) }
    factory { GetPresenceForListUseCase(get()) }

    // Activity feed (item 11) — DAO-backed, same direct-DAO pattern as CatalogUseCases.
    factory { RecordActivityUseCase(get(), get()) }
    factory { GetListActivityUseCase(get()) }
}
