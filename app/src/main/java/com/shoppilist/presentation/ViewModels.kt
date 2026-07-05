package com.shoppilist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.local.UserEntity
import com.shoppilist.data.session.SessionManager
import com.shoppilist.shared.domain.*
import com.shoppilist.shared.data.local.ShoppingListEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AuthViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    fun loginOrRegister(fullName: String?, email: String?, phone: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            val existing = if (!email.isNullOrBlank() || !phone.isNullOrBlank()) {
                userDao.findByContact(email?.takeIf { it.isNotBlank() }, phone?.takeIf { it.isNotBlank() })
            } else null

            val pendingLocale = sessionManager.consumePendingLocale()
            val base = existing ?: UserEntity(
                userId = UUID.randomUUID().toString(),
                fullName = fullName?.takeIf { it.isNotBlank() } ?: email ?: phone ?: "You",
                phone = phone?.takeIf { it.isNotBlank() },
                email = email?.takeIf { it.isNotBlank() },
                country = null,
                state = null,
                city = null,
                pincode = null,
                profileImageUrl = null
            )
            val user = if (pendingLocale != null) {
                base.copy(countryCode = pendingLocale.first, languageCode = pendingLocale.second)
            } else base
            userDao.upsert(user)
            sessionManager.setCurrentUser(user.userId)
            onDone()
        }
    }
}

class HomeViewModel(
    private val getAllListsUseCase: GetAllListsUseCase,
    private val createListUseCase: CreateListUseCase,
    private val archiveListUseCase: ArchiveListUseCase,
    private val togglePinUseCase: TogglePinUseCase,
    private val getListItemsUseCase: GetListItemsUseCase,
    private val getListMembersUseCase: GetListMembersUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _listMeta = MutableStateFlow<Map<String, ListMeta>>(emptyMap())
    val listMeta: StateFlow<Map<String, ListMeta>> = _listMeta

    init {
        viewModelScope.launch {
            getAllListsUseCase()
                .catch { e -> _uiState.value = _uiState.value.copy(error = e.message) }
                .collect { lists -> _uiState.value = _uiState.value.copy(lists = lists, loading = false) }
        }
        viewModelScope.launch {
            getAllListsUseCase()
                .flatMapLatest { lists ->
                    if (lists.isEmpty()) {
                        flowOf(emptyMap())
                    } else {
                        combine(
                            lists.map { list ->
                                combine(
                                    getListItemsUseCase(list.listId),
                                    getListMembersUseCase(list.listId)
                                ) { items, members -> list.listId to ListMeta(items.size, members.size) }
                            }
                        ) { pairs -> pairs.toMap() }
                    }
                }
                .catch { }
                .collect { _listMeta.value = it }
        }
    }

    fun createList(name: String, description: String?, colorHex: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val result = createListUseCase(name, description, ownerId = sessionManager.requireUserId(), colorHex = colorHex)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(loading = false)
            } else {
                _uiState.value = _uiState.value.copy(loading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun archiveList(listId: String) {
        viewModelScope.launch { archiveListUseCase(listId) }
    }

    fun togglePin(listId: String, pinned: Boolean) {
        viewModelScope.launch { togglePinUseCase(listId, pinned) }
    }
}

data class HomeUiState(
    val lists: List<ShoppingListEntity> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

data class ListMeta(val itemCount: Int, val memberCount: Int)

data class AmbiguousCategoryPrompt(val itemId: String, val itemName: String, val suggestedCategoryId: String)
data class LeftoverPrompt(val listId: String, val listName: String, val unchecked: List<ShoppingItemEntity>)

class ListDetailViewModel(
    private val getListUseCase: GetListUseCase,
    private val getListItemsUseCase: GetListItemsUseCase,
    private val getMyItemsUseCase: GetMyItemsUseCase,
    private val addItemUseCase: AddItemUseCase,
    private val markItemCheckedUseCase: MarkItemCheckedUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val clearPurchasedUseCase: ClearPurchasedUseCase,
    private val assignItemUseCase: AssignItemUseCase,
    private val unassignItemUseCase: UnassignItemUseCase,
    private val getListMembersUseCase: GetListMembersUseCase,
    private val getCategoriesForListUseCase: GetCategoriesForListUseCase,
    private val overrideItemCategoryUseCase: OverrideItemCategoryUseCase,
    private val renameCategoryForListUseCase: RenameCategoryForListUseCase,
    private val createCustomCategoryUseCase: CreateCustomCategoryUseCase,
    private val doneShoppingUseCase: DoneShoppingUseCase,
    private val createLeftoverListUseCase: CreateLeftoverListUseCase,
    private val createSubListUseCase: CreateSubListUseCase,
    private val markPresenceUseCase: MarkPresenceUseCase,
    private val getPresenceForListUseCase: GetPresenceForListUseCase,
    private val suggestionEngine: SuggestionEngine,
    private val getGroceryAppsForCountryUseCase: GetGroceryAppsForCountryUseCase,
    private val userDao: com.shoppilist.shared.data.local.UserDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    val currentUserId: String get() = sessionManager.requireUserId()

    private val _groceryApps = MutableStateFlow<List<com.shoppilist.shared.data.local.GroceryAppEntity>>(emptyList())
    val groceryApps: StateFlow<List<com.shoppilist.shared.data.local.GroceryAppEntity>> = _groceryApps

    private val _groceryCardDismissed = MutableStateFlow(false)
    val groceryCardDismissed: StateFlow<Boolean> = _groceryCardDismissed

    fun loadGroceryApps() {
        viewModelScope.launch {
            val user = userDao.getUserOnce(currentUserId)
            _groceryCardDismissed.value = user?.groceryCardDismissed ?: false
            val country = user?.countryCode ?: return@launch
            getGroceryAppsForCountryUseCase(country).collect { _groceryApps.value = it }
        }
    }

    fun dismissGroceryCard() {
        viewModelScope.launch {
            val user = userDao.getUserOnce(currentUserId) ?: return@launch
            userDao.upsert(user.copy(groceryCardDismissed = true))
            _groceryCardDismissed.value = true
        }
    }

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions

    fun refreshSuggestions(queryPrefix: String = "") {
        viewModelScope.launch {
            val countryCode = userDao.getUserOnce(currentUserId)?.countryCode
            _suggestions.value = suggestionEngine.getSuggestions(currentUserId, countryCode, queryPrefix)
        }
    }

    fun dismissSuggestion(itemName: String) {
        viewModelScope.launch {
            suggestionEngine.dismiss(currentUserId, itemName)
            refreshSuggestions()
        }
    }

    fun getList(listId: String) = getListUseCase(listId)
    fun getItems(listId: String) = getListItemsUseCase(listId)
    fun getMyItems(listId: String) = getMyItemsUseCase(listId, currentUserId)
    fun getMembers(listId: String) = getListMembersUseCase(listId)
    fun getCategories(listId: String) = getCategoriesForListUseCase(listId)
    fun getPresence(listId: String) = getPresenceForListUseCase(listId)

    fun markPresent(listId: String) {
        viewModelScope.launch { markPresenceUseCase(listId, currentUserId) }
    }

    suspend fun resolveUserName(userId: String): String? = userDao.getUserOnce(userId)?.fullName

    private val _ambiguousCategoryPrompt = MutableStateFlow<AmbiguousCategoryPrompt?>(null)
    val ambiguousCategoryPrompt: StateFlow<AmbiguousCategoryPrompt?> = _ambiguousCategoryPrompt

    fun addItem(listId: String, itemName: String, quantity: Double = 1.0, unit: String? = null, notes: String? = null) {
        viewModelScope.launch {
            val result = addItemUseCase(listId, itemName, quantity, unit, notes = notes, addedBy = currentUserId)
            result.getOrNull()?.let { added ->
                val ambiguousCategoryId = added.ambiguousCategoryId
                if (ambiguousCategoryId != null) {
                    _ambiguousCategoryPrompt.value = AmbiguousCategoryPrompt(added.itemId, itemName, ambiguousCategoryId)
                }
            }
            refreshSuggestions()
        }
    }

    fun confirmCategory(categoryId: String) {
        val prompt = _ambiguousCategoryPrompt.value ?: return
        viewModelScope.launch {
            overrideItemCategoryUseCase(prompt.itemId, prompt.itemName, prompt.suggestedCategoryId, categoryId, currentUserId)
            _ambiguousCategoryPrompt.value = null
        }
    }

    fun dismissCategoryPrompt() {
        _ambiguousCategoryPrompt.value = null
    }

    fun overrideCategory(itemId: String, itemName: String, previousCategoryId: String?, newCategoryId: String) {
        viewModelScope.launch {
            overrideItemCategoryUseCase(itemId, itemName, previousCategoryId, newCategoryId, currentUserId)
        }
    }

    fun renameCategory(listId: String, sourceCategoryId: String, newName: String) {
        viewModelScope.launch { renameCategoryForListUseCase(listId, sourceCategoryId, newName) }
    }

    fun createCustomCategory(listId: String, name: String, emoji: String, displayOrder: Int) {
        viewModelScope.launch { createCustomCategoryUseCase(listId, name, emoji, displayOrder) }
    }

    fun markChecked(itemId: String, checked: Boolean) {
        viewModelScope.launch {
            markItemCheckedUseCase(itemId, checked)
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            deleteItemUseCase(itemId)
        }
    }

    fun clearPurchased(listId: String) {
        viewModelScope.launch { clearPurchasedUseCase(listId) }
    }

    fun assignToSelf(itemId: String, itemName: String, listName: String) {
        viewModelScope.launch {
            assignItemUseCase(itemId, itemName, listName, currentUserId, currentUserId, "You")
        }
    }

    fun assignTo(itemId: String, itemName: String, listName: String, userId: String, assignerName: String) {
        viewModelScope.launch {
            assignItemUseCase(itemId, itemName, listName, userId, currentUserId, assignerName)
        }
    }

    fun unassign(itemId: String) {
        viewModelScope.launch { unassignItemUseCase(itemId) }
    }

    private val _leftoverPrompt = MutableStateFlow<LeftoverPrompt?>(null)
    val leftoverPrompt: StateFlow<LeftoverPrompt?> = _leftoverPrompt

    fun doneShopping(listId: String, listName: String) {
        viewModelScope.launch {
            val unchecked = doneShoppingUseCase(listId)
            if (unchecked.isNotEmpty()) {
                _leftoverPrompt.value = LeftoverPrompt(listId, listName, unchecked)
            }
        }
    }

    fun saveLeftoverList(leftoverName: String?) {
        val prompt = _leftoverPrompt.value ?: return
        viewModelScope.launch {
            createLeftoverListUseCase(prompt.listId, prompt.listName, currentUserId, leftoverName)
            _leftoverPrompt.value = null
        }
    }

    fun dismissLeftoverPrompt() {
        _leftoverPrompt.value = null
    }

    fun createSubList(parentListId: String, name: String, itemIds: List<String>) {
        viewModelScope.launch { createSubListUseCase(parentListId, name, currentUserId, itemIds) }
    }
}
