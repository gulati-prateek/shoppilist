package com.shoppilist.shared.backend

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
