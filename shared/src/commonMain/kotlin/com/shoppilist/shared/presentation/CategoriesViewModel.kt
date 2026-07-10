package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.data.local.ItemCategoryDao
import com.shoppilist.shared.data.local.ItemCategoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Backs the Categories tab: the global catalog taxonomy for browsing. */
class CategoriesViewModel(
    itemCategoryDao: ItemCategoryDao
) : ViewModel() {
    val categories: StateFlow<List<ItemCategoryEntity>> =
        itemCategoryDao.getGlobalCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
