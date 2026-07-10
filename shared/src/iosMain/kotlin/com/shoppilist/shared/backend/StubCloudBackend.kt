package com.shoppilist.shared.backend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * iOS placeholders, mirroring StubAuthService: keep the Koin graph resolvable and the shared UI
 * compiling for Kotlin/Native until a real iOS Firebase integration exists. Everything reports
 * unavailable; callers already treat that as "feature hidden".
 */
class StubCatalogBackend : CatalogBackend {
    override val isAvailable: Boolean = false

    override suspend fun fetchCatalog(region: String): Result<List<RemoteCatalogItem>> =
        Result.failure(IllegalStateException("Catalog backend isn't available on iOS yet"))

    override fun reportCustomItem(name: String, userId: String, userName: String?, countryCode: String?) = Unit
}

class StubProfileBackend : ProfileBackend {
    override val isAvailable: Boolean = false

    override fun saveProfile(profile: RemoteProfile) = Unit

    override suspend fun fetchProfile(uid: String): RemoteProfile? = null
}

class StubAdminBackend : AdminBackend {
    override val isAvailable: Boolean = false

    override suspend fun isAdmin(uid: String): Boolean = false

    override suspend fun pendingCustomItems(): Result<List<CustomItemReport>> =
        Result.failure(IllegalStateException("Admin backend isn't available on iOS yet"))

    override suspend fun approveCustomItem(
        reportId: String,
        finalName: String,
        categoryId: String,
        region: String
    ): Result<Unit> = Result.failure(IllegalStateException("Admin backend isn't available on iOS yet"))

    override suspend fun rejectCustomItem(reportId: String): Result<Unit> =
        Result.failure(IllegalStateException("Admin backend isn't available on iOS yet"))
}

class StubCollaborationBackend : CollaborationBackend {
    override val isAvailable: Boolean = false

    override fun pushList(list: RemoteList) = Unit
    override fun deleteList(listId: String) = Unit
    override fun pushItem(listId: String, item: RemoteListItem) = Unit
    override fun deleteItem(listId: String, itemId: String) = Unit
    override fun pushMember(listId: String, member: RemoteMember) = Unit
    override fun pushActivity(listId: String, activity: RemoteActivity) = Unit
    override fun createInvite(invite: RemoteInvite) = Unit

    override fun observeMyListIds(uid: String): Flow<List<String>> = flowOf(emptyList())
    override fun observeList(listId: String): Flow<RemoteList?> = flowOf(null)
    override fun observeItems(listId: String): Flow<List<RemoteListItem>> = flowOf(emptyList())
    override fun observeMembers(listId: String): Flow<List<RemoteMember>> = flowOf(emptyList())
    override fun observeActivity(listId: String): Flow<List<RemoteActivity>> = flowOf(emptyList())
    override fun observePendingInvites(contacts: List<String>): Flow<List<RemoteInvite>> = flowOf(emptyList())

    override suspend fun acceptInvite(invite: RemoteInvite, userId: String, userName: String?): Result<Unit> =
        Result.failure(IllegalStateException("Collaboration backend isn't available on iOS yet"))
}
