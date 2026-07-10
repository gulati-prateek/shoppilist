@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.backend.AdminBackend
import com.shoppilist.shared.backend.CustomItemReport
import com.shoppilist.shared.data.local.GlobalItemDao
import com.shoppilist.shared.data.local.GlobalItemEntity
import com.shoppilist.shared.data.local.ItemCategoryDao
import com.shoppilist.shared.data.local.ItemCategoryEntity
import com.shoppilist.shared.data.local.SponsoredRetailerDao
import com.shoppilist.shared.data.local.SponsoredRetailerEntity
import com.shoppilist.shared.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class AdminUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val info: String? = null,
    val isAdmin: Boolean = true,
    val reports: List<CustomItemReport> = emptyList(),
    val categories: List<ItemCategoryEntity> = emptyList()
)

/**
 * Admin dashboard: (1) the review queue of user-added off-catalog items; (2) affiliate-link /
 * vendor configuration with a per-vendor on/off switch (A1/A2); (3) catalog item management —
 * add/edit/remove items within a category (A4). Vendor + catalog edits apply to the local
 * database (the seeded source for those features on this device).
 */
class AdminViewModel(
    private val adminBackend: AdminBackend,
    private val itemCategoryDao: ItemCategoryDao,
    private val sponsoredRetailerDao: SponsoredRetailerDao,
    private val globalItemDao: GlobalItemDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state

    // A1/A2: all vendors with their affiliate on/off (isActive) state.
    val retailers: StateFlow<List<SponsoredRetailerEntity>> =
        sponsoredRetailerDao.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // A4: items in the selected category.
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId

    val categoryItems: StateFlow<List<GlobalItemEntity>> =
        _selectedCategoryId
            .flatMapLatest { cat -> if (cat == null) flowOf(emptyList()) else globalItemDao.getByCategoryFlow(cat) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            // A3: gate the whole panel on the signed-in account's admin marker.
            val admin = runCatching { adminBackend.isAdmin(sessionManager.requireUserId()) }.getOrDefault(false)
            _state.update { it.copy(isAdmin = admin) }
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

    fun selectCategory(categoryId: String?) { _selectedCategoryId.value = categoryId }

    // A2: start/close a vendor's affiliate program.
    fun setRetailerActive(id: String, active: Boolean) {
        viewModelScope.launch { sponsoredRetailerDao.setActive(id, active) }
    }

    // A4: add / edit / remove catalog items in a category.
    fun addCatalogItem(name: String, categoryId: String) {
        val trimmed = name.trim().lowercase()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            globalItemDao.insert(
                GlobalItemEntity(
                    id = "admin-${Uuid.random()}",
                    name = trimmed,
                    categoryId = categoryId,
                    region = "GLOBAL"
                )
            )
        }
    }

    fun updateCatalogItem(item: GlobalItemEntity, newName: String) {
        val trimmed = newName.trim().lowercase()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { globalItemDao.insert(item.copy(name = trimmed)) }
    }

    fun deleteCatalogItem(id: String) {
        viewModelScope.launch { globalItemDao.deleteById(id) }
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
