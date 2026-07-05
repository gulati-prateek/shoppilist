package com.shoppilist.shared.domain

import com.shoppilist.shared.data.local.GroceryAppEntity
import com.shoppilist.shared.data.local.SponsoredRetailerEntity
import com.shoppilist.shared.data.repository.GroceryAppRepository
import com.shoppilist.shared.data.repository.SponsoredRetailerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetRetailersForCountryUseCase(private val repo: SponsoredRetailerRepository) {
    operator fun invoke(countryCode: String): Flow<List<SponsoredRetailerEntity>> = repo.getForCountry(countryCode)
}

/** Splits a retailer list into sponsored vs. organic, honoring the user's "hide sponsored" preference (§2.13). */
class GetOrderOnlineOptionsUseCase(private val repo: SponsoredRetailerRepository) {
    operator fun invoke(countryCode: String, hideSponsored: Boolean): Flow<List<SponsoredRetailerEntity>> =
        repo.getForCountry(countryCode).map { all ->
            if (hideSponsored) all.filter { !it.isSponsored } else all
        }
}

class LogSponsoredClickUseCase(private val repo: SponsoredRetailerRepository) {
    suspend operator fun invoke(userId: String, retailerId: String, itemId: String?, listId: String?, clickType: String, countryCode: String) =
        repo.logClick(userId, retailerId, itemId, listId, clickType, countryCode)
}

class GetGroceryAppsForCountryUseCase(private val repo: GroceryAppRepository) {
    operator fun invoke(countryCode: String): Flow<List<GroceryAppEntity>> = repo.getForCountry(countryCode)
}
