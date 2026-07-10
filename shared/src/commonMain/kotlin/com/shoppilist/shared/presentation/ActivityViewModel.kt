package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.data.local.ListActivityEntity
import com.shoppilist.shared.domain.GetListActivityUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Backs the per-list activity feed (item 11): who added/removed/checked items, renamed the list,
 *  or joined — newest first. */
class ActivityViewModel(
    private val getListActivity: GetListActivityUseCase
) : ViewModel() {

    private val _activity = MutableStateFlow<List<ListActivityEntity>>(emptyList())
    val activity: StateFlow<List<ListActivityEntity>> = _activity

    fun load(listId: String) {
        viewModelScope.launch {
            getListActivity(listId).collect { _activity.value = it }
        }
    }
}
