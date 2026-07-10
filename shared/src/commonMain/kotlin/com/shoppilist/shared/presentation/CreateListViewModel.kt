package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.data.local.GlobalItemDao
import com.shoppilist.shared.data.local.ItemCategoryDao
import com.shoppilist.shared.data.local.ItemCategoryEntity
import com.shoppilist.shared.data.local.ItemHistoryDao
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.session.SessionManager
import com.shoppilist.shared.domain.AddItemUseCase
import com.shoppilist.shared.domain.CatalogRegion
import com.shoppilist.shared.domain.CreateListUseCase
import com.shoppilist.shared.domain.ResolveCatalogRegionUseCase
import com.shoppilist.shared.domain.SyncCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CreateListStep { PICK_ITEMS, NAME }

/** One category section of the create-list picker. */
data class CatalogSection(val category: ItemCategoryEntity, val itemNames: List<String>)

data class CreateListUiState(
    val loadingCatalog: Boolean = true,
    val step: CreateListStep = CreateListStep.PICK_ITEMS,
    val sections: List<CatalogSection> = emptyList(),
    /** This user's most-added item names, ranked by frequency (not alphabetical) — shown as a
     *  pinned section above every catalog category. */
    val frequentItems: List<String> = emptyList(),
    /** Canonical (lowercase) names ticked so far, in selection order. */
    val selected: Set<String> = emptySet(),
    /** User-typed names that aren't in the catalog (also members of [selected]). */
    val customItems: List<String> = emptyList(),
    val creating: Boolean = false,
    val error: String? = null,
    /** Set once the list exists — the UI navigates into it. */
    val createdListId: String? = null
)

/**
 * The redesigned create-list flow: items are picked FIRST (category-wise catalog with
 * checkboxes + free-text custom items), the name/color form comes after. One ViewModel spans
 * both steps so the selection never has to squeeze through navigation arguments.
 */
class CreateListViewModel(
    private val createListUseCase: CreateListUseCase,
    private val addItemUseCase: AddItemUseCase,
    private val globalItemDao: GlobalItemDao,
    private val itemCategoryDao: ItemCategoryDao,
    private val resolveCatalogRegion: ResolveCatalogRegionUseCase,
    private val syncCatalog: SyncCatalogUseCase,
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
    private val itemHistoryDao: ItemHistoryDao
) : ViewModel() {

    private val _state = MutableStateFlow(CreateListUiState())
    val state: StateFlow<CreateListUiState> = _state

    init {
        viewModelScope.launch {
            val userId = sessionManager.requireUserId()
            val region = resolveCatalogRegion()
            runCatching { syncCatalog(region) } // offline → cached/seed rows still render
            val regions = if (region == CatalogRegion.GLOBAL) {
                listOf(CatalogRegion.GLOBAL)
            } else {
                listOf(CatalogRegion.GLOBAL, region)
            }
            // Raw country code (not the coarse Firestore-sync region above) for the CSV catalog's
            // per-country scoping, same countryCodes mechanism SuggestionEngine already filters on.
            val countryCode = sessionManager.lastLocation()?.countryCode
                ?: userDao.getUserOnce(userId)?.countryCode
            val itemsByCategory = globalItemDao.getByRegions(regions)
                .filter { countryCode == null || it.countryCodes.isEmpty() || it.countryCodes.contains(countryCode) }
                .groupBy { it.categoryId }
            val categories = itemCategoryDao.getGlobalCategories().first()
            val frequentItems = itemHistoryDao.getTopItemNames(userId, limit = 20).map { it.itemName }
            _state.update { state ->
                state.copy(
                    loadingCatalog = false,
                    frequentItems = frequentItems,
                    sections = categories.mapNotNull { category ->
                        itemsByCategory[category.categoryId]
                            ?.map { it.name }?.distinct()?.sorted()
                            ?.let { names -> CatalogSection(category, names) }
                    }
                )
            }
        }
    }

    fun toggleItem(name: String) {
        _state.update { state ->
            val selected = if (name in state.selected) state.selected - name else state.selected + name
            state.copy(
                selected = selected,
                customItems = state.customItems.filter { it in selected || it != name }
            )
        }
    }

    /** Item 7: anything the catalog doesn't have can still be added by typing it. */
    fun addCustomItem(rawName: String) {
        val name = rawName.trim().lowercase()
        if (name.isEmpty()) return
        _state.update { state ->
            val alreadyInCatalog = state.sections.any { section -> name in section.itemNames }
            state.copy(
                selected = state.selected + name,
                customItems = if (alreadyInCatalog || name in state.customItems) state.customItems
                else state.customItems + name
            )
        }
    }

    fun proceedToName() {
        if (_state.value.selected.isEmpty()) {
            _state.update { it.copy(error = "Pick at least one item first") }
            return
        }
        _state.update { it.copy(step = CreateListStep.NAME, error = null) }
    }

    fun backToItems() {
        _state.update { it.copy(step = CreateListStep.PICK_ITEMS, error = null) }
    }

    fun create(name: String, description: String?, colorHex: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(error = "Give the list a name") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(creating = true, error = null) }
            val userId = sessionManager.requireUserId()
            createListUseCase(trimmed, description?.takeIf { it.isNotBlank() }, userId, colorHex)
                .onSuccess { listId ->
                    // AddItemUseCase per item keeps category matching, suggestion history, and
                    // off-catalog admin reporting identical to every other add path.
                    _state.value.selected.forEach { itemName ->
                        addItemUseCase(listId = listId, itemName = itemName, addedBy = userId)
                    }
                    _state.update { it.copy(creating = false, createdListId = listId) }
                }
                .onFailure { e ->
                    _state.update { it.copy(creating = false, error = e.message ?: "Couldn't create the list") }
                }
        }
    }
}
