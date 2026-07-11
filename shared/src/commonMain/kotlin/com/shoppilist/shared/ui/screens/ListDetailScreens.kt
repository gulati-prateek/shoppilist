@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.shoppilist.shared.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch
import com.shoppilist.shared.currentTimeMillis
import com.shoppilist.shared.data.local.ItemCategoryEntity
import com.shoppilist.shared.data.local.ListMemberEntity
import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.presentation.AmbiguousCategoryPrompt
import com.shoppilist.shared.presentation.ListDetailViewModel

private enum class ListViewMode { TO_GET, CART, AISLE, MY_ITEMS }

private data class CategoryGroup(
    val categoryId: String,
    val name: String,
    val emoji: String,
    val displayOrder: Int,
    val items: List<ShoppingItemEntity>
)

private fun groupByCategory(items: List<ShoppingItemEntity>, categoryById: Map<String, ItemCategoryEntity>): List<CategoryGroup> {
    return items.groupBy { it.categoryId }
        .map { (categoryId, groupItems) ->
            val category = categoryId?.let { categoryById[it] }
            CategoryGroup(
                categoryId = categoryId ?: "uncategorized",
                name = category?.name ?: "Other",
                emoji = category?.emoji ?: "🛒",
                displayOrder = category?.displayOrder ?: Int.MAX_VALUE,
                items = groupItems
            )
        }
        .sortedBy { it.displayOrder }
}

private fun formatQty(q: Double): String = if (q == q.toLong().toDouble()) q.toLong().toString() else q.toString()

@Composable
fun ListDetailScreen(
    listId: String,
    viewModel: ListDetailViewModel = koinViewModel(),
    onOpenItem: (String) -> Unit = {},
    onOrderWholeList: (String) -> Unit = {},
    onOpenAssignments: (String) -> Unit = {},
    onOpenActivity: (String) -> Unit = {},
    onInvite: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val list by viewModel.getList(listId).collectAsState(initial = null)
    val items by viewModel.getItems(listId).collectAsState(initial = emptyList())
    val myItems by viewModel.getMyItems(listId).collectAsState(initial = emptyList())
    val members by viewModel.getMembers(listId).collectAsState(initial = emptyList())
    val categories by viewModel.getCategories(listId).collectAsState(initial = emptyList())
    val presence by viewModel.getPresence(listId).collectAsState(initial = emptyList())
    val suggestions by viewModel.suggestions.collectAsState()
    val ambiguousPrompt by viewModel.ambiguousCategoryPrompt.collectAsState()
    val leftoverPrompt by viewModel.leftoverPrompt.collectAsState()
    val groceryApps by viewModel.groceryApps.collectAsState()
    val groceryCardDismissed by viewModel.groceryCardDismissed.collectAsState()
    val catalogSections by viewModel.catalogSections.collectAsState()
    val catalogFrequentItems by viewModel.catalogFrequentItems.collectAsState()
    val catalogLoading by viewModel.catalogLoading.collectAsState()

    val haptic = LocalHapticFeedback.current
    val currentUserId = viewModel.currentUserId

    var viewMode by remember { mutableStateOf(ListViewMode.TO_GET) }
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showNotesField by remember { mutableStateOf(false) }
    var selectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showSubListDialog by remember { mutableStateOf(false) }
    var subListName by remember { mutableStateOf("") }
    var leftoverName by remember { mutableStateOf("") }
    var assigneeTargetItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var editTargetItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var showCatalogPicker by remember { mutableStateOf(false) }
    var showModeInfo by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf("You") }
    var showRenameDialog by remember { mutableStateOf(false) }
    // L3/L8: To-Get items ticked but not yet moved to Cart (explicit "Move to Cart" step).
    var stagedForCart by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(listId) {
        viewModel.markPresent(listId)
        viewModel.refreshSuggestions()
        viewModel.loadGroceryApps()
    }
    LaunchedEffect(currentUserId) {
        currentUserName = viewModel.resolveUserName(currentUserId) ?: "You"
    }

    val otherActivePresence = remember(presence, currentUserId) {
        presence.firstOrNull { p ->
            p.userId != currentUserId && (currentTimeMillis() - p.lastActiveAt) < 2 * 60 * 1000
        }
    }
    var otherPresenceName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(otherActivePresence?.userId) {
        otherPresenceName = otherActivePresence?.userId?.let { viewModel.resolveUserName(it) }
    }

    val categoryById = remember(categories) { categories.associateBy { it.categoryId } }
    val hasChecked = items.any { it.checked }

    // B6: a VIEWER member gets a read-only list — mutation affordances hidden/inert here, and the
    // Firestore rules enforce the same server-side. No member row (solo local list) = full access.
    val myRole = remember(members, currentUserId) { members.find { it.userId == currentUserId }?.role }
    val readOnly = myRole == com.shoppilist.shared.data.local.ListRole.VIEWER

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (selectMode) "${selectedIds.size} selected" else list?.name ?: "List") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectMode) {
                            selectMode = false
                            selectedIds = emptySet()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(if (selectMode) Icons.Default.Close else Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenActivity(listId) }) {
                        Icon(Icons.Default.History, contentDescription = "Activity")
                    }
                    IconButton(onClick = { onInvite(listId) }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add people")
                    }
                    IconButton(onClick = {
                        selectMode = !selectMode
                        selectedIds = emptySet()
                    }) {
                        Icon(Icons.Default.Checklist, contentDescription = "Select items")
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        if (!readOnly) DropdownMenuItem(
                            text = { Text("Rename list") },
                            onClick = { menuExpanded = false; showRenameDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Who's Getting What") },
                            onClick = { menuExpanded = false; onOpenAssignments(listId) }
                        )
                        DropdownMenuItem(
                            text = { Text("Order All Online") },
                            onClick = { menuExpanded = false; onOrderWholeList(listId) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (selectMode && selectedIds.isNotEmpty()) {
                BottomAppBar {
                    Button(
                        onClick = { showSubListDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text("Group into Sub-list (${selectedIds.size})")
                    }
                }
            } else if (stagedForCart.isNotEmpty()) {
                // L3/L8: commit the staged To-Get items into the Cart in one explicit step.
                BottomAppBar {
                    Button(
                        onClick = {
                            stagedForCart.forEach { viewModel.markChecked(it, true) }
                            stagedForCart = emptySet()
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Move to Cart (${stagedForCart.size})")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (otherActivePresence != null) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${otherPresenceName ?: "Someone"} is shopping now 🛒",
                        modifier = Modifier.padding(12.dp, 6.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            MembersRosterRow(
                members = members,
                resolveName = { viewModel.resolveUserName(it) },
                onOrderOnline = { onOrderWholeList(listId) },
                onDoneShopping = { viewModel.doneShopping(listId, list?.name ?: "") }
            )

            if (readOnly) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "👁 View only — you were invited as a viewer, so you can't change this list.",
                        modifier = Modifier.padding(12.dp, 6.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!readOnly) AddItemSection(
                itemName = itemName,
                onNameChange = { itemName = it; viewModel.refreshSuggestions(it) },
                quantity = quantity,
                onQuantityChange = { quantity = it },
                unit = unit,
                onUnitChange = { unit = it },
                notes = notes,
                onNotesChange = { notes = it },
                showNotes = showNotesField,
                onToggleNotes = { showNotesField = !showNotesField },
                onSubmit = {
                    if (itemName.isNotBlank()) {
                        viewModel.addItem(
                            listId,
                            itemName.trim(),
                            quantity.toDoubleOrNull() ?: 1.0,
                            unit.trim().ifBlank { null },
                            notes.trim().ifBlank { null }
                        )
                        itemName = ""
                        quantity = "1"
                        unit = ""
                        notes = ""
                        showNotesField = false
                    }
                }
            )

            // Browse the region catalog to add items without typing each one.
            if (!readOnly) OutlinedButton(
                onClick = {
                    showCatalogPicker = true
                    viewModel.loadCatalog()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Checklist, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Browse items to add")
            }

            val modes = ListViewMode.entries.toList()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = viewMode == mode,
                            onClick = { viewMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                        ) {
                            Text(
                                when (mode) {
                                    ListViewMode.TO_GET -> "To Get"
                                    ListViewMode.CART -> "Cart"
                                    ListViewMode.AISLE -> "Aisle"
                                    ListViewMode.MY_ITEMS -> "My Items"
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
                IconButton(onClick = { showModeInfo = true }) {
                    Icon(Icons.Outlined.Info, contentDescription = "What do these views mean?")
                }
            }

            // Ticking a To-Get item stages it (L3/L8) — it moves to Cart only via the bottom-bar step.
            val onCheck: (ShoppingItemEntity) -> Unit = { item ->
                if (!readOnly) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    stagedForCart = if (item.itemId in stagedForCart) stagedForCart - item.itemId else stagedForCart + item.itemId
                }
            }
            val onUncheck: (ShoppingItemEntity) -> Unit = { item ->
                if (!readOnly) {
                    stagedForCart = stagedForCart - item.itemId
                    viewModel.markChecked(item.itemId, false)
                }
            }
            val onToggleSelect: (String) -> Unit = { id ->
                selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
            }

            Box(modifier = Modifier.weight(1f)) {
                when (viewMode) {
                    // "To Get" and "Cart" are separate top-level tabs (user request) instead of
                    // two sections stacked vertically inside one "List" view.
                    ListViewMode.TO_GET -> GroupedListModeContent(
                        title = "To Get",
                        items = items.filter { !it.checked },
                        emptyHint = "Nothing left to get 🎉 — add items above, or see what's in the Cart tab.",
                        categoryById = categoryById,
                        selectMode = selectMode,
                        selectedIds = selectedIds,
                        onToggleSelect = onToggleSelect,
                        onCheck = onCheck,
                        onUncheck = onUncheck,
                        onOpenItem = onOpenItem,
                        onOpenAssignee = { assigneeTargetItem = it },
                        resolveName = { viewModel.resolveUserName(it) },
                        staged = stagedForCart,
                        onEdit = { if (!readOnly) editTargetItem = it },
                        onQuantityChange = { id, d -> if (!readOnly) viewModel.changeQuantity(id, d) }
                    )
                    ListViewMode.CART -> GroupedListModeContent(
                        title = "In Cart",
                        items = items.filter { it.checked },
                        emptyHint = "Your cart is empty — tick items in To Get, then tap \"Move to Cart\".",
                        categoryById = categoryById,
                        selectMode = selectMode,
                        selectedIds = selectedIds,
                        onToggleSelect = onToggleSelect,
                        onCheck = onCheck,
                        onUncheck = onUncheck,
                        onOpenItem = onOpenItem,
                        onOpenAssignee = { assigneeTargetItem = it },
                        resolveName = { viewModel.resolveUserName(it) },
                        staged = stagedForCart,
                        onEdit = { if (!readOnly) editTargetItem = it },
                        onQuantityChange = { id, d -> if (!readOnly) viewModel.changeQuantity(id, d) }
                    )
                    ListViewMode.AISLE -> AisleModeContent(
                        items = items,
                        categoryById = categoryById,
                        selectMode = selectMode,
                        selectedIds = selectedIds,
                        onToggleSelect = onToggleSelect,
                        onCheck = onCheck,
                        onUncheck = onUncheck,
                        onOpenItem = onOpenItem,
                        onOpenAssignee = { assigneeTargetItem = it },
                        resolveName = { viewModel.resolveUserName(it) },
                        staged = stagedForCart,
                        onEdit = { if (!readOnly) editTargetItem = it },
                        onQuantityChange = { id, d -> if (!readOnly) viewModel.changeQuantity(id, d) }
                    )
                    ListViewMode.MY_ITEMS -> AisleModeContent(
                        items = myItems,
                        categoryById = categoryById,
                        selectMode = selectMode,
                        selectedIds = selectedIds,
                        onToggleSelect = onToggleSelect,
                        onCheck = onCheck,
                        onUncheck = onUncheck,
                        onOpenItem = onOpenItem,
                        onOpenAssignee = { assigneeTargetItem = it },
                        resolveName = { viewModel.resolveUserName(it) },
                        staged = stagedForCart,
                        onEdit = { if (!readOnly) editTargetItem = it },
                        onQuantityChange = { id, d -> if (!readOnly) viewModel.changeQuantity(id, d) }
                    )
                }
            }

            if (hasChecked && !readOnly) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    OutlinedButton(onClick = { viewModel.clearPurchased(listId) }) { Text("Clear Purchased") }
                }
            }
        }
    }

    val activePrompt = ambiguousPrompt
    if (activePrompt != null) {
        CategoryAmbiguityDialog(
            prompt = activePrompt,
            categories = categories,
            onPick = { viewModel.confirmCategory(it) },
            onDismiss = { viewModel.dismissCategoryPrompt() }
        )
    }

    val activeLeftover = leftoverPrompt
    if (activeLeftover != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLeftoverPrompt(); leftoverName = "" },
            title = { Text("Save leftover items?") },
            text = {
                Column {
                    Text("You have ${activeLeftover.unchecked.size} items left. Save to a new list?")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = leftoverName,
                        onValueChange = { leftoverName = it },
                        label = { Text("New list name (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveLeftoverList(leftoverName.trim().ifBlank { null })
                    leftoverName = ""
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLeftoverPrompt(); leftoverName = "" }) { Text("Cancel") }
            }
        )
    }

    if (showSubListDialog) {
        AlertDialog(
            onDismissRequest = { showSubListDialog = false },
            title = { Text("New sub-list name") },
            text = {
                OutlinedTextField(
                    value = subListName,
                    onValueChange = { subListName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (subListName.isNotBlank()) {
                        viewModel.createSubList(listId, subListName.trim(), selectedIds.toList())
                        subListName = ""
                        selectedIds = emptySet()
                        selectMode = false
                        showSubListDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showSubListDialog = false }) { Text("Cancel") } }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(list?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename list") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("List name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameList(listId, newName)
                        showRenameDialog = false
                    },
                    enabled = newName.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
        )
    }

    val targetItem = assigneeTargetItem
    if (targetItem != null) {
        AssigneeDialog(
            item = targetItem,
            members = members,
            currentUserId = currentUserId,
            resolveName = { userId -> viewModel.resolveUserName(userId) },
            onAssignSelf = { viewModel.assignToSelf(targetItem.itemId, targetItem.name, list?.name ?: "") },
            onAssignTo = { userId -> viewModel.assignTo(targetItem.itemId, targetItem.name, list?.name ?: "", userId, currentUserName) },
            onUnassign = { viewModel.unassign(targetItem.itemId) },
            onDismiss = { assigneeTargetItem = null }
        )
    }

    val editItem = editTargetItem
    if (editItem != null) {
        EditItemDialog(
            item = editItem,
            onSave = { qty, unitValue, notesValue ->
                viewModel.updateItemDetails(editItem.itemId, qty, unitValue, notesValue)
                editTargetItem = null
            },
            onAssign = {
                assigneeTargetItem = editItem
                editTargetItem = null
            },
            onRemove = {
                viewModel.deleteItem(editItem.itemId)
                editTargetItem = null
            },
            onDismiss = { editTargetItem = null }
        )
    }

    if (showCatalogPicker) {
        val existingNames = remember(items) { items.map { it.name.trim().lowercase() }.toSet() }
        CatalogPickerSheet(
            loading = catalogLoading,
            sections = catalogSections,
            frequentItems = catalogFrequentItems,
            existingNames = existingNames,
            onAdd = { name -> viewModel.addItem(listId, name) },
            onDismiss = { showCatalogPicker = false }
        )
    }

    if (showModeInfo) {
        AlertDialog(
            onDismissRequest = { showModeInfo = false },
            title = { Text("List views") },
            text = {
                Column {
                    ModeInfoLine("To Get", "What's still to pick up, grouped by category. Tick items to stage them, then tap \"Move to Cart\".")
                    Spacer(Modifier.height(8.dp))
                    ModeInfoLine("Cart", "Everything already in your cart. Untick an item to send it back to To Get.")
                    Spacer(Modifier.height(8.dp))
                    ModeInfoLine("Aisle", "Every item grouped by store aisle (category) in aisle order, so you shop row by row.")
                    Spacer(Modifier.height(8.dp))
                    ModeInfoLine("My Items", "Only the items assigned to you.")
                }
            },
            confirmButton = { TextButton(onClick = { showModeInfo = false }) { Text("Got it") } }
        )
    }
}

@Composable
private fun ModeInfoLine(title: String, description: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CatalogPickerSheet(
    loading: Boolean,
    sections: List<com.shoppilist.shared.presentation.CatalogSection>,
    frequentItems: List<String>,
    existingNames: Set<String>,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Names added during this picker session (so the row shows a persistent "Added" state).
    var addedThisSession by remember { mutableStateOf(setOf<String>()) }
    var search by remember { mutableStateOf("") }
    val query = search.trim().lowercase()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    val visibleFrequent = if (query.isEmpty()) frequentItems else frequentItems.filter { it.contains(query) }
    val visibleSections = sections.mapNotNull { section ->
        val visible = if (query.isEmpty()) section.itemNames else section.itemNames.filter { it.contains(query) }
        if (visible.isNotEmpty()) section to visible else null
    }

    // Flat LazyColumn index of a category header, mirroring the render order below, so a chip tap
    // scrolls the item list straight to that category.
    fun indexOfCategory(categoryId: String): Int {
        var idx = 0
        if (visibleFrequent.isNotEmpty()) idx += 1 + visibleFrequent.size
        for ((section, visible) in visibleSections) {
            if (section.category.categoryId == categoryId) return idx
            idx += 1 + visible.size
        }
        return idx
    }

    fun addName(name: String) {
        onAdd(name)
        addedThisSession = addedThisSession + name.trim().lowercase()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                    Text("Browse items", style = MaterialTheme.typography.titleLarge)
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search items") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                // Category quick-jump chips → scroll the item list to that category.
                if (visibleSections.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleSections, key = { it.first.category.categoryId }) { (section, _) ->
                            AssistChip(
                                onClick = {
                                    scope.launch { listState.animateScrollToItem(indexOfCategory(section.category.categoryId)) }
                                },
                                label = { Text("${section.category.emoji} ${section.category.name}") }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    if (visibleFrequent.isNotEmpty()) {
                        item(key = "cat-frequent-header") { CatalogPickerHeader("⭐", "Frequently Added") }
                        items(visibleFrequent, key = { "cat-frequent-$it" }) { name ->
                            CatalogPickerRow(
                                name = name,
                                added = name.trim().lowercase() in addedThisSession,
                                alreadyInList = name.trim().lowercase() in existingNames,
                                onAdd = { addName(name) }
                            )
                        }
                    }
                    visibleSections.forEach { (section, visible) ->
                        item(key = "cat-header-${section.category.categoryId}") {
                            CatalogPickerHeader(section.category.emoji, section.category.name)
                        }
                        items(visible, key = { "cat-${section.category.categoryId}-$it" }) { name ->
                            CatalogPickerRow(
                                name = name,
                                added = name.trim().lowercase() in addedThisSession,
                                alreadyInList = name.trim().lowercase() in existingNames,
                                onAdd = { addName(name) },
                                categoryId = section.category.categoryId,
                                categoryEmoji = section.category.emoji
                            )
                        }
                    }
                    if (!loading && visibleFrequent.isEmpty() && visibleSections.isEmpty()) {
                        item(key = "cat-empty") {
                            Text(
                                if (query.isEmpty()) "No catalog items available yet." else "No items match \"$search\".",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun CatalogPickerHeader(emoji: String, name: String) {
    Text(
        "$emoji  $name",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun CatalogPickerRow(
    name: String,
    added: Boolean,
    alreadyInList: Boolean,
    onAdd: () -> Unit,
    categoryId: String? = null,
    categoryEmoji: String? = null
) {
    val done = added || alreadyInList
    ListItem(
        headlineContent = {
            Text(name.replaceFirstChar { it.uppercase() })
        },
        leadingContent = {
            com.shoppilist.shared.ui.components.ItemIcon(
                name = name, categoryId = categoryId, size = 36.dp, categoryEmoji = categoryEmoji
            )
        },
        trailingContent = {
            if (done) {
                Text(
                    if (alreadyInList && !added) "In list" else "Added",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            } else {
                IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add $name") }
            }
        },
        modifier = if (done) Modifier else Modifier.clickable(onClick = onAdd)
    )
}

@Composable
private fun EditItemDialog(
    item: ShoppingItemEntity,
    onSave: (Double, String?, String?) -> Unit,
    onAssign: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var quantity by remember(item.itemId) { mutableStateOf(formatQty(item.quantity)) }
    var unit by remember(item.itemId) { mutableStateOf(item.unit ?: "") }
    var notes by remember(item.itemId) { mutableStateOf(item.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${item.name}") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Qty") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onAssign) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Assign to person")
                }
                TextButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Remove from list")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    quantity.toDoubleOrNull() ?: item.quantity,
                    unit.trim().ifBlank { null },
                    notes.trim().ifBlank { null }
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddItemSection(
    itemName: String,
    onNameChange: (String) -> Unit,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    showNotes: Boolean,
    onToggleNotes: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = itemName,
                onValueChange = onNameChange,
                label = { Text("Add item") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = quantity,
                onValueChange = onQuantityChange,
                label = { Text("Qty") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(72.dp)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = unit,
                onValueChange = onUnitChange,
                label = { Text("Unit") },
                singleLine = true,
                modifier = Modifier.width(80.dp)
            )
            IconButton(onClick = onSubmit) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        }
        TextButton(onClick = onToggleNotes) {
            Text(if (showNotes) "Hide notes" else "Add notes")
        }
        if (showNotes) {
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SuggestionChipsRow(suggestions: List<String>, onSelect: (String) -> Unit, onDismiss: (String) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions, key = { it }) { name ->
            InputChip(
                selected = false,
                onClick = { onSelect(name) },
                label = { Text(name) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss suggestion",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDismiss(name) }
                    )
                }
            )
        }
    }
}

@Composable
private fun CategoryAmbiguityDialog(
    prompt: AmbiguousCategoryPrompt,
    categories: List<ItemCategoryEntity>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val suggested = categories.find { it.categoryId == prompt.suggestedCategoryId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What category is \"${prompt.itemName}\"?") },
        text = {
            Column {
                Text("Is it ${suggested?.name ?: "this"} or something else?")
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        AssistChip(
                            onClick = { onPick(category.categoryId) },
                            label = { Text("${category.emoji} ${category.name}") }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CategoryHeaderRow(group: CategoryGroup, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${group.emoji} ${group.name} (${group.items.size})",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand"
        )
    }
}

@Composable
private fun GroupedListModeContent(
    title: String,
    items: List<ShoppingItemEntity>,
    emptyHint: String,
    categoryById: Map<String, ItemCategoryEntity>,
    selectMode: Boolean,
    selectedIds: Set<String>,
    onToggleSelect: (String) -> Unit,
    onCheck: (ShoppingItemEntity) -> Unit,
    onUncheck: (ShoppingItemEntity) -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenAssignee: (ShoppingItemEntity) -> Unit,
    resolveName: suspend (String) -> String? = { null },
    staged: Set<String> = emptySet(),
    onEdit: (ShoppingItemEntity) -> Unit = {},
    onQuantityChange: (String, Double) -> Unit = { _, _ -> }
) {
    // One tab = one section: "To Get" and "Cart" are separate top-level view modes now, so this
    // renders a single pre-filtered set of items grouped by category.
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val groups = remember(items, categoryById) { groupByCategory(items, categoryById) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "section_header") {
            Text(
                "$title (${items.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp, 8.dp)
            )
        }
        if (items.isEmpty()) {
            item(key = "empty_hint") {
                Text(
                    emptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }
        }
        groups.forEach { group ->
            val key = group.categoryId
            val isExpanded = expanded[key] ?: true
            item(key = "hdr_$key") {
                CategoryHeaderRow(group, isExpanded) { expanded[key] = !isExpanded }
            }
            if (isExpanded) {
                items(group.items, key = { it.itemId }) { itemEntity ->
                    ItemRow(itemEntity, selectMode, itemEntity.itemId in selectedIds, onToggleSelect, onCheck, onUncheck, onOpenItem, onOpenAssignee, resolveName, itemEntity.categoryId?.let { categoryById[it]?.name }, staged, onEdit, onQuantityChange)
                }
            }
        }
    }
}

@Composable
private fun AisleModeContent(
    items: List<ShoppingItemEntity>,
    categoryById: Map<String, ItemCategoryEntity>,
    selectMode: Boolean,
    selectedIds: Set<String>,
    onToggleSelect: (String) -> Unit,
    onCheck: (ShoppingItemEntity) -> Unit,
    onUncheck: (ShoppingItemEntity) -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenAssignee: (ShoppingItemEntity) -> Unit,
    resolveName: suspend (String) -> String? = { null },
    staged: Set<String> = emptySet(),
    onEdit: (ShoppingItemEntity) -> Unit = {},
    onQuantityChange: (String, Double) -> Unit = { _, _ -> }
) {
    val groups = remember(items, categoryById) { groupByCategory(items, categoryById) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { group ->
            stickyHeader(key = "h_${group.categoryId}") {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${group.emoji} ${group.name}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            items(group.items, key = { it.itemId }) { itemEntity ->
                ItemRow(itemEntity, selectMode, itemEntity.itemId in selectedIds, onToggleSelect, onCheck, onUncheck, onOpenItem, onOpenAssignee, resolveName, itemEntity.categoryId?.let { categoryById[it]?.name }, staged, onEdit, onQuantityChange)
            }
        }
    }
}

/** Item 12: horizontal roster of list members' avatars with a "+ add people" affordance at the end. */
@Composable
private fun MembersRosterRow(
    members: List<ListMemberEntity>,
    resolveName: suspend (String) -> String?,
    onOrderOnline: () -> Unit,
    onDoneShopping: () -> Unit
) {
    var names by remember(members) { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(members) {
        val map = mutableMapOf<String, String>()
        members.forEach { m -> resolveName(m.userId)?.let { map[m.userId] = it } }
        names = map
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // L5: contributors on this list (Add-People lives only on the top bar now — L6).
        Column(modifier = Modifier.weight(1f)) {
            Text("Contributing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                members.take(6).forEach { m ->
                    val name = names[m.userId]
                    com.shoppilist.shared.ui.components.ProfileAvatar(
                        initial = name?.firstOrNull()?.toString(),
                        seed = m.userId,
                        size = 30.dp,
                        modifier = Modifier.border(1.5.dp, MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape)
                    )
                }
                if (members.isEmpty()) {
                    Text("Just you", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        // Order Online + Done Shopping sit together.
        AssistChip(
            onClick = onOrderOnline,
            leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp)) },
            label = { Text("Order Online") }
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = onDoneShopping, contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text("Done Shopping")
        }
    }
}

@Composable
private fun ItemRow(
    item: ShoppingItemEntity,
    selectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: (String) -> Unit,
    onCheck: (ShoppingItemEntity) -> Unit,
    onUncheck: (ShoppingItemEntity) -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenAssignee: (ShoppingItemEntity) -> Unit,
    resolveName: suspend (String) -> String? = { null },
    categoryLabel: String? = null,
    staged: Set<String> = emptySet(),
    onEdit: (ShoppingItemEntity) -> Unit = {},
    onQuantityChange: (String, Double) -> Unit = { _, _ -> }
) {
    val isStaged = item.itemId in staged
    // Item 15: show who marked this item purchased ("✓ by Alice").
    var checkedByName by remember(item.itemId, item.checkedBy) { mutableStateOf<String?>(null) }
    LaunchedEffect(item.checkedBy, item.checked) {
        checkedByName = if (item.checked) item.checkedBy?.let { resolveName(it) } else null
    }
    ListItem(
        headlineContent = {
            Text(
                item.name,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val qtyLine = "${formatQty(item.quantity)} ${item.unit ?: ""}".trim()
                    if (qtyLine.isNotBlank()) {
                        Text(qtyLine, style = MaterialTheme.typography.bodySmall)
                        if (categoryLabel != null) Spacer(Modifier.width(8.dp))
                    }
                    if (categoryLabel != null) {
                        // Mockup: a soft category tag (e.g. "Groceries") beside the quantity.
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                categoryLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                val notes = item.notes
                if (!notes.isNullOrBlank()) Text(notes, style = MaterialTheme.typography.bodySmall)
                if (item.checked) {
                    // L2: a visible way to move a Cart item back to To-Get (not just long-press).
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (checkedByName != null) {
                            Text(
                                "✓ by $checkedByName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            "↩ Move to To-Get",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.clickable { onUncheck(item) }
                        )
                    }
                } else {
                    Text(
                        text = if (item.assignedTo != null) "Assigned" else "+ Assign",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.assignedTo != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.clickable { onOpenAssignee(item) }
                    )
                }
            }
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    selectMode -> Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect(item.itemId) })
                    item.checked -> Checkbox(checked = true, onCheckedChange = { onUncheck(item) })
                    // L3/L8: ticking a To-Get item STAGES it (doesn't move to Cart); the explicit
                    // "Move to Cart" bottom-bar step commits the staged items.
                    else -> Checkbox(checked = isStaged, onCheckedChange = { onCheck(item) })
                }
                com.shoppilist.shared.ui.components.ItemIcon(
                    name = item.name,
                    categoryId = item.categoryId,
                    size = 40.dp
                )
            }
        },
        trailingContent = if (selectMode) null else {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quantity steppers (never below 1).
                    IconButton(onClick = { onQuantityChange(item.itemId, -1.0) }, enabled = item.quantity > 1.0) {
                        Icon(Icons.Default.Remove, "Decrease quantity")
                    }
                    Text(formatQty(item.quantity), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { onQuantityChange(item.itemId, 1.0) }) {
                        Icon(Icons.Default.Add, "Increase quantity")
                    }
                    IconButton(onClick = { onEdit(item) }) {
                        Icon(Icons.Default.Edit, "Edit item")
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    when {
                        selectMode -> onToggleSelect(item.itemId)
                        !item.checked -> onCheck(item)
                        else -> onOpenItem(item.itemId)
                    }
                },
                onLongClick = {
                    if (!selectMode && item.checked) onUncheck(item)
                }
            ),
        colors = when {
            isSelected && selectMode -> ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            isStaged -> ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            else -> ListItemDefaults.colors()
        }
    )
}

@Composable
private fun AssigneeDialog(
    item: ShoppingItemEntity,
    members: List<ListMemberEntity>,
    currentUserId: String,
    resolveName: suspend (String) -> String?,
    onAssignSelf: () -> Unit,
    onAssignTo: (String) -> Unit,
    onUnassign: () -> Unit,
    onDismiss: () -> Unit
) {
    var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(members) {
        val map = mutableMapOf<String, String>()
        members.forEach { m -> resolveName(m.userId)?.let { map[m.userId] = it } }
        names = map
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign \"${item.name}\"") },
        text = {
            Column {
                TextButton(onClick = { onAssignSelf(); onDismiss() }) { Text("I'll get this") }
                if (item.assignedTo != null) {
                    TextButton(onClick = { onUnassign(); onDismiss() }) { Text("Unassign") }
                }
                HorizontalDivider()
                members.filter { it.userId != currentUserId }.forEach { member ->
                    TextButton(onClick = { onAssignTo(member.userId); onDismiss() }) {
                        Text(names[member.userId] ?: member.userId)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
