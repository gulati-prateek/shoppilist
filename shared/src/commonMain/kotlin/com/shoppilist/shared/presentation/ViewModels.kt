@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.auth.AuthService
import com.shoppilist.shared.auth.AuthUser
import com.shoppilist.shared.backend.ProfileBackend
import com.shoppilist.shared.backend.RemoteProfile
import com.shoppilist.shared.backend.RemoteInvite
import com.shoppilist.shared.data.local.GlobalItemDao
import com.shoppilist.shared.data.local.InvitationEntity
import com.shoppilist.shared.data.local.ItemCategoryDao
import com.shoppilist.shared.data.local.ItemHistoryDao
import com.shoppilist.shared.data.local.ListActivityAction
import com.shoppilist.shared.data.local.ListRole
import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.sync.CollaborationSyncManager
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.local.UserEntity
import com.shoppilist.shared.data.session.SessionManager
import com.shoppilist.shared.data.session.StoredLocation
import com.shoppilist.shared.domain.*
import com.shoppilist.shared.data.local.ShoppingListEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    /** An SMS code was sent; show the OTP entry field. */
    val otpSent: Boolean = false,
    /** Account exists but the email link hasn't been clicked yet; show the verify panel. */
    val awaitingEmailVerification: Boolean = false,
    val registeredEmail: String? = null,
    /** First-time account (no first name on record yet) — route to profile setup, not Home. */
    val needsProfile: Boolean = false,
    /** Set only once the account is verified (issue 9's gate) — the UI navigates on this. */
    val verifiedUser: AuthUser? = null
)

class AuthViewModel(
    private val authService: AuthService,
    private val accountSync: UserAccountSync
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    /** Name captured at registration time; phone verification completes via async callbacks
     *  that no longer have the form fields in scope. */
    private var pendingFullName: String? = null

    fun registerWithEmail(fullName: String?, email: String, password: String) {
        val trimmed = email.trim()
        if (!trimmed.contains("@")) { fail("Enter a valid email address"); return }
        if (password.length < 6) { fail("Password must be at least 6 characters"); return }
        pendingFullName = fullName
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            authService.registerWithEmail(trimmed, password)
                .onSuccess { user ->
                    accountSync.ensureLocalUser(user, fullName)
                    // Send explicitly so a rejected send (quota, throttling…) surfaces here
                    // instead of the UI claiming the email went out when it didn't.
                    val sent = authService.sendEmailVerification()
                    _state.update {
                        it.copy(
                            loading = false,
                            awaitingEmailVerification = true,
                            registeredEmail = trimmed,
                            info = if (sent.isSuccess) "Verification email sent to $trimmed" else null,
                            error = sent.exceptionOrNull()?.let { e ->
                                "Account created, but the verification email couldn't be sent: " +
                                    "${e.message ?: "unknown error"}. Tap Resend to try again."
                            }
                        )
                    }
                }
                .onFailure { e -> fail(e.message ?: "Registration failed") }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank() || password.isBlank()) { fail("Enter your email and password"); return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            authService.signInWithEmail(trimmed, password)
                .onSuccess { user ->
                    val local = accountSync.ensureLocalUser(user, null)
                    if (user.isVerified) {
                        complete(user, local)
                    } else {
                        _state.update {
                            it.copy(
                                loading = false,
                                awaitingEmailVerification = true,
                                registeredEmail = trimmed,
                                info = "Your email isn't verified yet — tap the link we sent you"
                            )
                        }
                    }
                }
                .onFailure { e -> fail(e.message ?: "Login failed") }
        }
    }

    /** "I've clicked the link" — re-reads the account from the server. */
    fun refreshVerificationStatus() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val user = authService.currentUser(refresh = true)
            if (user != null && user.isVerified) {
                val local = accountSync.ensureLocalUser(user, pendingFullName)
                complete(user, local)
            } else {
                fail("Not verified yet — tap the link in the email first (check spam too)")
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            authService.sendEmailVerification()
                .onSuccess { _state.update { it.copy(loading = false, info = "Verification email sent again") } }
                .onFailure { e -> fail(e.message ?: "Couldn't send the email") }
        }
    }

    /** Phase 3: complete Google sign-in once the platform UI returns an ID token. */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            authService.signInWithGoogle(idToken)
                .onSuccess { user ->
                    val localUser = accountSync.ensureLocalUser(user, null)
                    complete(user, localUser)
                }
                .onFailure { e -> fail(e.message ?: "Google sign-in failed") }
        }
    }

    /** Item 5: email password-reset link. */
    fun sendPasswordReset(email: String) {
        val trimmed = email.trim()
        if (!trimmed.contains("@")) { fail("Enter your account email first"); return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            authService.sendPasswordReset(trimmed)
                .onSuccess { _state.update { it.copy(loading = false, info = "Reset link sent — check your email") } }
                .onFailure { e -> fail(e.message ?: "Couldn't send the reset link") }
        }
    }

    fun sendOtp(fullName: String?, phoneNumber: String, uiHost: Any?) {
        val phone = phoneNumber.trim().replace(" ", "")
        if (!phone.startsWith("+") || phone.length < 8) {
            fail("Enter the full number with country code, e.g. +919876543210")
            return
        }
        pendingFullName = fullName
        _state.update { it.copy(loading = true, error = null, info = null) }
        authService.startPhoneVerification(
            phoneNumber = phone,
            uiHost = uiHost,
            onCodeSent = { _state.update { it.copy(loading = false, otpSent = true, info = "Code sent to $phone") } },
            onVerified = ::onPhoneVerified,
            onError = ::fail
        )
    }

    fun submitOtp(code: String) {
        if (code.isBlank()) { fail("Enter the code from the SMS"); return }
        _state.update { it.copy(loading = true, error = null, info = null) }
        authService.submitOtp(code.trim(), onVerified = ::onPhoneVerified, onError = ::fail)
    }

    private fun onPhoneVerified(user: AuthUser) {
        viewModelScope.launch {
            val local = accountSync.ensureLocalUser(user, pendingFullName)
            complete(user, local)
        }
    }

    private fun fail(message: String) {
        _state.update { it.copy(loading = false, error = message) }
    }

    private fun complete(user: AuthUser, localUser: UserEntity) {
        _state.update {
            it.copy(
                loading = false,
                error = null,
                needsProfile = localUser.firstName.isNullOrBlank(),
                verifiedUser = user
            )
        }
    }
}

class HomeViewModel(
    private val getAllListsUseCase: GetAllListsUseCase,
    private val archiveListUseCase: ArchiveListUseCase,
    private val togglePinUseCase: TogglePinUseCase,
    private val deleteListUseCase: DeleteListUseCase,
    private val getListItemsUseCase: GetListItemsUseCase,
    private val getListMembersUseCase: GetListMembersUseCase,
    private val renameListUseCase: RenameListUseCase,
    private val getPendingInvitesForContactUseCase: GetPendingInvitesForContactUseCase,
    private val acceptInviteUseCase: AcceptInviteUseCase,
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
    private val profileBackend: ProfileBackend,
    private val resolveCatalogRegion: ResolveCatalogRegionUseCase,
    private val syncCatalog: SyncCatalogUseCase,
    private val collaborationSync: CollaborationSyncManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _listMeta = MutableStateFlow<Map<String, ListMeta>>(emptyMap())
    val listMeta: StateFlow<Map<String, ListMeta>> = _listMeta

    /** Signed-in user, for the dashboard profile avatar (initials / blank). */
    val currentUser: StateFlow<UserEntity?> =
        userDao.getUserFlow(sessionManager.requireUserId())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Last fetched device location (dashboard chip) — restored from storage on relogin. */
    private val _lastLocation = MutableStateFlow(sessionManager.lastLocation())
    val lastLocation: StateFlow<StoredLocation?> = _lastLocation

    init {
        // Phase 4: begin mirroring shared lists from Firestore into local Room.
        collaborationSync.start()
        // Keep the region catalog cache warm so the create-list picker opens instantly.
        viewModelScope.launch {
            runCatching { syncCatalog(resolveCatalogRegion()) }
        }
        viewModelScope.launch {
            getAllListsUseCase()
                .catch { e -> _uiState.value = _uiState.value.copy(error = e.message) }
                .collect { lists -> _uiState.value = _uiState.value.copy(lists = lists, loading = false) }
        }
        viewModelScope.launch {
            getAllListsUseCase()
                .flatMapLatest { lists ->
                    if (lists.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        combine(
                            lists.map { list ->
                                combine(
                                    getListItemsUseCase(list.listId),
                                    getListMembersUseCase(list.listId)
                                ) { items, members -> Triple(list.listId, items, members) }
                            }
                        ) { it.toList() }
                    }
                }
                .catch { }
                .collect { triples ->
                    // Resolve member initials here (collect is suspend, unlike combine's transform)
                    // so cards can show collaborator avatars without a separate name lookup in the UI.
                    _listMeta.value = triples.associate { (listId, items, members) ->
                        val avatars = members.take(3).map { m ->
                            val name = userDao.getUserOnce(m.userId)?.fullName?.trim().orEmpty()
                            MemberAvatar(
                                initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                seed = m.userId
                            )
                        }
                        listId to ListMeta(
                            itemCount = items.size,
                            checkedCount = items.count { it.checked },
                            memberCount = members.size,
                            memberAvatars = avatars
                        )
                    }
                }
        }
    }

    /** Saves a freshly fetched location everywhere it's remembered: the session store (chip on
     *  relogin), the local profile row, and the cloud profile mirror — then refreshes the
     *  catalog for the (possibly changed) region. */
    fun saveLocation(location: StoredLocation) {
        sessionManager.setLastLocation(location)
        _lastLocation.value = location
        viewModelScope.launch {
            val userId = sessionManager.requireUserId()
            userDao.getUserOnce(userId)?.let { user ->
                userDao.upsert(
                    user.copy(
                        city = location.city ?: user.city,
                        state = location.state ?: user.state,
                        countryCode = location.countryCode ?: user.countryCode
                    )
                )
            }
            profileBackend.saveProfile(RemoteProfile(uid = userId, lastLocation = location))
            runCatching { syncCatalog(resolveCatalogRegion()) }
        }
    }

    fun archiveList(listId: String) {
        viewModelScope.launch { archiveListUseCase(listId) }
    }

    fun togglePin(listId: String, pinned: Boolean) {
        viewModelScope.launch { togglePinUseCase(listId, pinned) }
    }

    fun deleteList(listId: String) {
        viewModelScope.launch { deleteListUseCase(listId) }
    }

    fun renameList(listId: String, newName: String) {
        viewModelScope.launch { renameListUseCase(listId, newName) }
    }

    /** Cross-device Firestore invites addressed to me, kept for accept-routing. */
    private val remoteInvitesById = MutableStateFlow<Map<String, RemoteInvite>>(emptyMap())

    /** Pending invites addressed to the signed-in user's own email/phone (items 13/14) — merged
     *  from local Room and cross-device Firestore (Phase 4). */
    val pendingInvites: StateFlow<List<InvitationEntity>> =
        userDao.getUserFlow(sessionManager.requireUserId())
            .flatMapLatest { user ->
                val contacts = listOfNotNull(user?.email, user?.phone).filter { it.isNotBlank() }
                if (contacts.isEmpty()) return@flatMapLatest flowOf(emptyList())
                val local = combine(contacts.map { getPendingInvitesForContactUseCase(it) }) { it.toList().flatten() }
                val remote = collaborationSync.observePendingInvites(contacts)
                combine(local, remote) { localInvites, remoteInvites ->
                    remoteInvitesById.value = remoteInvites.associateBy { it.id }
                    (localInvites + remoteInvites.map { it.toInvitationEntity() }).distinctBy { it.inviteId }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun acceptInvite(invite: InvitationEntity) {
        viewModelScope.launch {
            val remote = remoteInvitesById.value[invite.inviteId]
            if (remote != null) collaborationSync.acceptInvite(remote)
            else acceptInviteUseCase(invite.token, sessionManager.requireUserId())
        }
    }

    private fun RemoteInvite.toInvitationEntity() = InvitationEntity(
        inviteId = id, listId = listId, inviterUserId = inviterUserId, inviteeContact = inviteeContact,
        channel = channel, role = runCatching { ListRole.valueOf(role) }.getOrDefault(ListRole.EDITOR),
        token = id, status = status, expiresAt = expiresAt, createdAt = createdAt
    )
}

data class HomeUiState(
    val lists: List<ShoppingListEntity> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

/** One collaborator's avatar seed for a list card: [initial] shown on a color derived from [seed]. */
data class MemberAvatar(val initial: String, val seed: String)

data class ListMeta(
    val itemCount: Int,
    val checkedCount: Int = 0,
    val memberCount: Int = 0,
    val memberAvatars: List<MemberAvatar> = emptyList()
)

data class AmbiguousCategoryPrompt(val itemId: String, val itemName: String, val suggestedCategoryId: String)
data class LeftoverPrompt(val listId: String, val listName: String, val unchecked: List<ShoppingItemEntity>)

class ListDetailViewModel(
    private val getListUseCase: GetListUseCase,
    private val getListItemsUseCase: GetListItemsUseCase,
    private val getMyItemsUseCase: GetMyItemsUseCase,
    private val addItemUseCase: AddItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
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
    private val getItemOnceUseCase: GetItemOnceUseCase,
    private val renameListUseCase: RenameListUseCase,
    private val recordActivity: RecordActivityUseCase,
    private val collaborationSync: CollaborationSyncManager,
    private val userDao: com.shoppilist.shared.data.local.UserDao,
    private val sessionManager: SessionManager,
    private val globalItemDao: GlobalItemDao,
    private val itemCategoryDao: ItemCategoryDao,
    private val itemHistoryDao: ItemHistoryDao,
    private val resolveCatalogRegion: ResolveCatalogRegionUseCase,
    private val syncCatalog: SyncCatalogUseCase
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
                runCatching { recordActivity(listId, currentUserId, ListActivityAction.ADDED_ITEM, itemName = itemName) }
                getItemOnceUseCase(added.itemId)?.let { collaborationSync.pushItem(listId, it) }
                val ambiguousCategoryId = added.ambiguousCategoryId
                if (ambiguousCategoryId != null) {
                    _ambiguousCategoryPrompt.value = AmbiguousCategoryPrompt(added.itemId, itemName, ambiguousCategoryId)
                }
            }
            refreshSuggestions()
        }
    }

    /** Item 10: rename the current list, recorded in the activity feed. */
    fun renameList(listId: String, newName: String) {
        viewModelScope.launch {
            renameListUseCase(listId, newName).onSuccess {
                runCatching { recordActivity(listId, currentUserId, ListActivityAction.RENAMED_LIST, detail = newName.trim()) }
            }
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
            markItemCheckedUseCase(itemId, checked, checkedBy = currentUserId)
            // Re-read after the update so the pushed snapshot carries the new checked/checkedBy state.
            val item = getItemOnceUseCase(itemId)
            item?.let {
                val action = if (checked) ListActivityAction.CHECKED_ITEM else ListActivityAction.UNCHECKED_ITEM
                runCatching { recordActivity(it.listId, currentUserId, action, itemName = it.name) }
                collaborationSync.pushItem(it.listId, it)
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            val item = getItemOnceUseCase(itemId)
            deleteItemUseCase(itemId)
            item?.let {
                runCatching { recordActivity(it.listId, currentUserId, ListActivityAction.DELETED_ITEM, itemName = it.name) }
                collaborationSync.deleteItem(it.listId, it.itemId)
            }
        }
    }

    fun clearPurchased(listId: String) {
        viewModelScope.launch { clearPurchasedUseCase(listId) }
    }

    /** Increment/decrement an item's quantity (never below 1). */
    fun changeQuantity(itemId: String, delta: Double) {
        viewModelScope.launch {
            val item = getItemOnceUseCase(itemId) ?: return@launch
            val newQty = (item.quantity + delta).coerceAtLeast(1.0)
            val updated = item.copy(quantity = newQty)
            updateItemUseCase(updated)
            collaborationSync.pushItem(item.listId, updated)
        }
    }

    /** Edit an item's quantity / unit / notes from the edit dialog. */
    fun updateItemDetails(itemId: String, quantity: Double, unit: String?, notes: String?) {
        viewModelScope.launch {
            val item = getItemOnceUseCase(itemId) ?: return@launch
            val updated = item.copy(quantity = quantity.coerceAtLeast(1.0), unit = unit, notes = notes)
            updateItemUseCase(updated)
            runCatching { recordActivity(item.listId, currentUserId, ListActivityAction.EDITED_ITEM, itemName = item.name) }
            collaborationSync.pushItem(item.listId, updated)
        }
    }

    // --- Browse-catalog picker (add items to an existing list, same source as create-list) ---
    private val _catalogSections = MutableStateFlow<List<CatalogSection>>(emptyList())
    val catalogSections: StateFlow<List<CatalogSection>> = _catalogSections

    private val _catalogFrequentItems = MutableStateFlow<List<String>>(emptyList())
    val catalogFrequentItems: StateFlow<List<String>> = _catalogFrequentItems

    private val _catalogLoading = MutableStateFlow(false)
    val catalogLoading: StateFlow<Boolean> = _catalogLoading

    /** Loads the region catalog once for the "Browse items" picker (cached after first open). */
    fun loadCatalog() {
        if (_catalogSections.value.isNotEmpty() || _catalogLoading.value) return
        viewModelScope.launch {
            _catalogLoading.value = true
            val userId = currentUserId
            val region = resolveCatalogRegion()
            runCatching { syncCatalog(region) } // offline → cached/seed rows still render
            val regions = if (region == CatalogRegion.GLOBAL) listOf(CatalogRegion.GLOBAL)
            else listOf(CatalogRegion.GLOBAL, region)
            val countryCode = sessionManager.lastLocation()?.countryCode
                ?: userDao.getUserOnce(userId)?.countryCode
            val itemsByCategory = globalItemDao.getByRegions(regions)
                .filter { countryCode == null || it.countryCodes.isEmpty() || it.countryCodes.contains(countryCode) }
                .groupBy { it.categoryId }
            val categories = itemCategoryDao.getGlobalCategories().first()
            _catalogFrequentItems.value = itemHistoryDao.getTopItemNames(userId, limit = 20).map { it.itemName }
            _catalogSections.value = categories.mapNotNull { category ->
                itemsByCategory[category.categoryId]
                    ?.map { it.name }?.distinct()?.sorted()
                    ?.let { names -> CatalogSection(category, names) }
            }
            _catalogLoading.value = false
        }
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
