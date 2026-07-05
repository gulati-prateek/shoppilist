@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.data.repository

import com.shoppilist.shared.data.local.GroceryAppDao
import com.shoppilist.shared.data.local.GroceryAppEntity
import com.shoppilist.shared.data.local.SponsoredClickDao
import com.shoppilist.shared.data.local.SponsoredClickEntity
import com.shoppilist.shared.data.local.SponsoredRetailerDao
import com.shoppilist.shared.data.local.SponsoredRetailerEntity
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface SponsoredRetailerRepository {
    fun getForCountry(countryCode: String): Flow<List<SponsoredRetailerEntity>>
    suspend fun logClick(userId: String, retailerId: String, itemId: String?, listId: String?, clickType: String, countryCode: String)
}

class RoomSponsoredRetailerRepository(
    private val retailerDao: SponsoredRetailerDao,
    private val clickDao: SponsoredClickDao
) : SponsoredRetailerRepository {

    override fun getForCountry(countryCode: String): Flow<List<SponsoredRetailerEntity>> =
        retailerDao.getForCountry(countryCode)

    override suspend fun logClick(
        userId: String,
        retailerId: String,
        itemId: String?,
        listId: String?,
        clickType: String,
        countryCode: String
    ) {
        clickDao.insert(
            SponsoredClickEntity(
                id = Uuid.random().toString(),
                userId = userId,
                retailerId = retailerId,
                itemId = itemId,
                listId = listId,
                clickType = clickType,
                countryCode = countryCode
            )
        )
    }
}

interface GroceryAppRepository {
    fun getForCountry(countryCode: String): Flow<List<GroceryAppEntity>>
}

class RoomGroceryAppRepository(private val groceryAppDao: GroceryAppDao) : GroceryAppRepository {
    override fun getForCountry(countryCode: String): Flow<List<GroceryAppEntity>> = groceryAppDao.getForCountry(countryCode)
}
