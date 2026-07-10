package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.backend.AdminBackend
import com.shoppilist.shared.backend.CustomItemReport
import com.shoppilist.shared.data.local.ItemCategoryDao
import com.shoppilist.shared.data.local.ItemCategoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val info: String? = null,
    val reports: List<CustomItemReport> = emptyList(),
    val categories: List<ItemCategoryEntity> = emptyList()
)

/**
 * Admin dashboard v1 (deliberately minimal, to be grown gradually): the review queue of
 * user-added off-catalog items. Approving one — optionally after editing its name and picking
 * a category/region — publishes it into the remote master catalog for everyone in that region.
 */
class AdminViewModel(
    private val adminBackend: AdminBackend,
    private val itemCategoryDao: ItemCategoryDao
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val categories = itemCategoryDao.getGlobalCategories().first()
            adminBackend.pendingCustomItems()
                .onSuccess { reports ->
                    _state.update { it.copy(loading = false, reports = reports, categories = categories) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loading = false, categories = categories, error = e.message ?: "Couldn't load reports")
                    }
                }
        }
    }

    fun approve(report: CustomItemReport, finalName: String, categoryId: String, region: String) {
        viewModelScope.launch {
            adminBackend.approveCustomItem(report.id, finalName, categoryId, region)
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            reports = state.reports.filterNot { it.id == report.id },
                            info = "\"${finalName.trim()}\" added to the $region catalog"
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "Approval failed") } }
        }
    }

    fun reject(report: CustomItemReport) {
        viewModelScope.launch {
            adminBackend.rejectCustomItem(report.id)
                .onSuccess {
                    _state.update { state ->
                        state.copy(reports = state.reports.filterNot { it.id == report.id })
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "Reject failed") } }
        }
    }
}
