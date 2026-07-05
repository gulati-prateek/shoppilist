package com.shoppilist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.affiliate.RetailerUrlBuilder
import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.data.local.SponsoredRetailerEntity
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.data.session.SessionManager
import com.shoppilist.shared.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val DEFAULT_COUNTRY = "US"

class OrderOnlineViewModel(
    private val getOrderOnlineOptionsUseCase: GetOrderOnlineOptionsUseCase,
    private val logSponsoredClickUseCase: LogSponsoredClickUseCase,
    private val getItemOnceUseCase: GetItemOnceUseCase,
    private val getListItemsUseCase: GetListItemsUseCase,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _item = MutableStateFlow<ShoppingItemEntity?>(null)
    val item: StateFlow<ShoppingItemEntity?> = _item

    private val _retailers = MutableStateFlow<List<SponsoredRetailerEntity>>(emptyList())
    val retailers: StateFlow<List<SponsoredRetailerEntity>> = _retailers

    private var countryCode: String = DEFAULT_COUNTRY

    fun loadForItem(itemId: String) {
        viewModelScope.launch {
            _item.value = getItemOnceUseCase(itemId)
            loadRetailers()
        }
    }

    fun loadRetailersOnly() {
        viewModelScope.launch { loadRetailers() }
    }

    private suspend fun loadRetailers() {
        val user = userDao.getUserOnce(sessionManager.requireUserId())
        countryCode = user?.countryCode ?: DEFAULT_COUNTRY
        val hideSponsored = user?.hideSponsoredLinks ?: false
        getOrderOnlineOptionsUseCase(countryCode, hideSponsored).collect { _retailers.value = it }
    }

    fun buildSearchUrl(retailer: SponsoredRetailerEntity, itemName: String): String =
        RetailerUrlBuilder.searchUrl(retailer, itemName)

    fun logClick(retailer: SponsoredRetailerEntity, itemId: String?, listId: String?, clickType: String) {
        viewModelScope.launch {
            logSponsoredClickUseCase(sessionManager.requireUserId(), retailer.id, itemId, listId, clickType, countryCode)
        }
    }

    fun buildWholeListPlan(retailer: SponsoredRetailerEntity, itemNames: List<String>): RetailerUrlBuilder.WholeListPlan =
        RetailerUrlBuilder.wholeListPlan(retailer, itemNames)

    suspend fun getItemNamesForList(listId: String): List<String> =
        getListItemsUseCase(listId).first().filter { !it.checked }.map { it.name }
}
