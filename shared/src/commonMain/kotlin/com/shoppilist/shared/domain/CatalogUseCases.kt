package com.shoppilist.shared.domain

import com.shoppilist.shared.backend.CatalogBackend
import com.shoppilist.shared.currentTimeMillis
import com.shoppilist.shared.data.local.GlobalItemDao
import com.shoppilist.shared.data.local.GlobalItemEntity
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.session.SessionManager

/**
 * Which remote catalog this user should see: the GPS-detected country wins (dashboard location
 * chip), the onboarding/settings country is the fallback, GLOBAL when neither is known.
 */
class ResolveCatalogRegionUseCase(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): String {
        val gpsCountry = sessionManager.lastLocation()?.countryCode
        val chosenCountry = userDao.getUserOnce(sessionManager.requireUserId())?.countryCode
        return CatalogRegion.forCountry(gpsCountry ?: chosenCountry)
    }
}

/**
 * Refreshes the local cache of one region's remote catalog into `global_items` (region-tagged
 * rows, replaced wholesale). Skips when the cache is fresh; failures (offline, backend
 * unavailable) leave the existing cache untouched — the bundled GLOBAL seed always remains.
 */
class SyncCatalogUseCase(
    private val globalItemDao: GlobalItemDao,
    private val catalogBackend: CatalogBackend,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(region: String, force: Boolean = false) {
        if (region !in CatalogRegion.REMOTE_REGIONS || !catalogBackend.isAvailable) return
        val syncedAt = sessionManager.catalogSyncedAt(region)
        if (!force && syncedAt != null && currentTimeMillis() - syncedAt < STALE_AFTER_MS) return
        catalogBackend.fetchCatalog(region).onSuccess { items ->
            if (items.isNotEmpty()) {
                globalItemDao.deleteByRegion(region)
                globalItemDao.insertAll(items.map { remote ->
                    GlobalItemEntity(
                        // Region-prefixed so remote ids can never collide with the GLOBAL seed's.
                        id = "$region-${remote.id}",
                        name = remote.name,
                        categoryId = remote.categoryId,
                        countryCodes = emptyList(),
                        languageTranslations = remote.translations,
                        isSeasonal = remote.isSeasonal,
                        seasonMonths = remote.seasonMonths,
                        region = region
                    )
                })
                sessionManager.setCatalogSyncedAt(region, currentTimeMillis())
            }
        }
    }

    companion object {
        const val STALE_AFTER_MS = 24 * 60 * 60 * 1000L
    }
}
