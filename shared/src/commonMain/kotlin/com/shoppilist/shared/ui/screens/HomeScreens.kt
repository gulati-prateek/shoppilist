@file:OptIn(ExperimentalMaterial3Api::class)

package com.shoppilist.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.shoppilist.shared.data.local.InvitationEntity
import com.shoppilist.shared.data.local.ShoppingListEntity
import com.shoppilist.shared.data.local.seed.CategoryIds
import com.shoppilist.shared.location.rememberLocationController
import com.shoppilist.shared.presentation.CreateListStep
import com.shoppilist.shared.presentation.CreateListViewModel
import com.shoppilist.shared.presentation.HomeViewModel
import com.shoppilist.shared.presentation.ListMeta
import com.shoppilist.shared.ui.components.ProfileAvatar

/** The five top-level shopping domains the catalog spans — surfaced on the dashboard to signal the
 *  app isn't grocery-only. Each maps to the category ids a quick-start list would emphasize. */
private data class ShopDomain(val emoji: String, val label: String, val categoryId: String)
private val SHOP_DOMAINS = listOf(
    // categoryId = the first catalog category of that domain, so tapping the chip opens the
    // create-list catalog scrolled to where the domain's sections begin.
    ShopDomain("🛒", "Grocery", CategoryIds.FRESH_PRODUCE),
    ShopDomain("👗", "Fashion", CategoryIds.MENS_WEAR),
    ShopDomain("📱", "Electronics", CategoryIds.MOBILE_ACCESSORIES),
    ShopDomain("🏠", "Home", CategoryIds.HOME_APPLIANCES),
    ShopDomain("🎁", "Gifts", CategoryIds.GIFTS_STATIONERY)
)

private val PRESET_LIST_COLORS = listOf("#2ECC71", "#3B82F6", "#F59E0B", "#EF4444", "#8B5CF6", "#6B7280")

// android.graphics.Color.parseColor has no Kotlin/Native equivalent, so hex parsing is hand-rolled.
private fun colorFromHex(hex: String?): Color? {
    val cleaned = hex?.takeIf { it.isNotBlank() }?.removePrefix("#") ?: return null
    return try {
        val argb = when (cleaned.length) {
            6 -> (0xFF000000L or cleaned.toLong(16))
            8 -> cleaned.toLong(16)
            else -> return null
        }
        Color(argb.toInt())
    } catch (e: NumberFormatException) {
        null
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    /** Opens create-list; a non-null categoryId scrolls its catalog to that category. */
    onCreateList: (String?) -> Unit = {},
    onOpenList: (String) -> Unit = {},
    onOpenVoice: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    /** When hosted inside the bottom-nav shell, the shell owns the create FAB. */
    showFab: Boolean = true
) {
    val state by viewModel.uiState.collectAsState()
    val listMeta by viewModel.listMeta.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val pendingInvites by viewModel.pendingInvites.collectAsState()
    var renameTarget by remember { mutableStateOf<ShoppingListEntity?>(null) }

    renameTarget?.let { target ->
        RenameListDialog(
            currentName = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.renameList(target.listId, newName)
                renameTarget = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenProfile) {
                        val name = currentUser?.fullName
                        ProfileAvatar(
                            initial = name?.firstOrNull()?.toString(),
                            seed = currentUser?.userId ?: "me",
                            size = 32.dp
                        )
                    }
                },
                title = { Text("ShoppiList") }
            )
        },
        floatingActionButton = {
            if (showFab) {
                ExtendedFloatingActionButton(
                    onClick = { onCreateList(null) },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("New list") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Location first, directly under the ShoppiList top bar (D1).
                item(key = "location") { LocationChipRow(viewModel) }
                item(key = "tagline") {
                    Text(
                        "Shop anything, together",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp)
                    )
                }
                // Tapping a domain opens create-list scrolled to that domain's first category.
                item(key = "domains") { ShopDomainStrip(onPick = { onCreateList(it.categoryId) }) }

                if (pendingInvites.isNotEmpty()) {
                    item(key = "invites-header") {
                        Text(
                            "Invitations",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(pendingInvites, key = { "invite-${it.inviteId}" }) { invite ->
                        PendingInviteCard(
                            invite = invite,
                            onAccept = { viewModel.acceptInvite(invite) }
                        )
                    }
                }

                if (!state.loading && state.lists.isEmpty()) {
                    item(key = "empty") { EmptyListsState() }
                } else {
                    item(key = "lists-header") {
                        Text(
                            "Your lists",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(state.lists, key = { it.listId }) { list ->
                        HomeListCard(
                            list = list,
                            meta = listMeta[list.listId],
                            onOpen = { onOpenList(list.listId) },
                            onArchive = { viewModel.archiveList(list.listId) },
                            onTogglePin = { viewModel.togglePin(list.listId, !list.pinned) },
                            onRename = { renameTarget = list },
                            onDelete = { viewModel.deleteList(list.listId) }
                        )
                    }
                }

                if (state.error != null) {
                    item(key = "error") {
                        Text("Error: ${state.error}", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

/** Horizontal strip of shopping-domain chips communicating the app spans grocery→electronics. */
@Composable
private fun ShopDomainStrip(onPick: (ShopDomain) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SHOP_DOMAINS, key = { it.label }) { domain ->
            AssistChip(
                onClick = { onPick(domain) },
                label = { Text("${domain.emoji} ${domain.label}") }
            )
        }
    }
}

@Composable
private fun EmptyListsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛍️", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text("No lists yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap \"New list\" to start shopping — groceries, fashion, electronics and more.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PendingInviteCard(invite: InvitationEntity, onAccept: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("You've been invited to a list", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Invited as ${invite.role.name.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Button(onClick = onAccept) { Text("Accept") }
        }
    }
}

@Composable
private fun RenameListDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename list") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("List name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Item 5: dashboard location row. Shows the remembered location (survives relogin via
 * SessionManager); tapping fetches a fresh fix (asking for permission the first time) and
 * persists it locally + to the cloud profile.
 */
@Composable
private fun LocationChipRow(viewModel: HomeViewModel) {
    val lastLocation by viewModel.lastLocation.collectAsState()
    var fetching by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }

    val locationController = rememberLocationController(
        onLocation = { fetched ->
            fetching = false
            locationError = null
            viewModel.saveLocation(fetched)
        },
        onError = { message ->
            fetching = false
            locationError = message
        }
    )
    if (!locationController.isAvailable) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        AssistChip(
            onClick = {
                locationError = null
                fetching = true
                locationController.requestLocation()
            },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            label = {
                val place = lastLocation?.let { loc ->
                    listOfNotNull(loc.city, loc.state ?: loc.countryCode).joinToString(", ")
                }
                Text(
                    when {
                        fetching -> "Locating…"
                        !place.isNullOrBlank() -> place
                        else -> "Set my location"
                    }
                )
            }
        )
        locationError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HomeListCard(
    list: ShoppingListEntity,
    meta: ListMeta?,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete list?") },
            text = { Text("\"${list.name}\" and all its items will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }

    val itemCount = meta?.itemCount ?: 0
    val checkedCount = meta?.checkedCount ?: 0
    val progress = if (itemCount > 0) checkedCount.toFloat() / itemCount else 0f

    ElevatedCard(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                colorFromHex(list.colorHex)?.let { color ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    list.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onTogglePin) {
                    Icon(
                        if (list.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (list.pinned) "Unpin" else "Pin to top",
                        tint = if (list.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        // "Edit list" opens the list itself (same as tapping the card); renaming
                        // is its own explicit action.
                        DropdownMenuItem(
                            text = { Text("Edit list") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuOpen = false; onOpen() }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename list") },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                            onClick = { menuOpen = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            leadingIcon = { Icon(Icons.Default.Archive, null) },
                            onClick = { menuOpen = false; onArchive() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; confirmDelete = true }
                        )
                    }
                }
            }

            val description = list.description
            if (!description.isNullOrBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (itemCount > 0) "$checkedCount/$itemCount done" else "No items yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                CollaboratorAvatars(meta)
            }
        }
    }
}

/** Overlapping collaborator avatars on a list card; "+N" when there are more than shown. */
@Composable
private fun CollaboratorAvatars(meta: ListMeta?) {
    val avatars = meta?.memberAvatars ?: emptyList()
    val extra = (meta?.memberCount ?: 0) - avatars.size
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        avatars.forEach { a ->
            ProfileAvatar(
                initial = a.initial,
                seed = a.seed,
                size = 26.dp,
                modifier = Modifier.border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("+$extra", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Item 8's redesigned flow: step 1 picks items from the region's category-wise catalog
 * (checkboxes on the left, plus free-text "other" items — item 7); step 2 names the list.
 * On create it lands inside the new list.
 */
@Composable
fun CreateListScreen(
    viewModel: CreateListViewModel = koinViewModel(),
    initialCategoryId: String? = null,
    onBack: () -> Unit,
    onCreated: (listId: String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.createdListId) {
        state.createdListId?.let(onCreated)
    }

    when (state.step) {
        CreateListStep.PICK_ITEMS -> PickItemsStep(viewModel, onBack, initialCategoryId)
        CreateListStep.NAME -> NameListStep(viewModel)
    }
}

@Composable
private fun PickItemsStep(
    viewModel: CreateListViewModel,
    onBack: () -> Unit,
    initialCategoryId: String? = null
) {
    val state by viewModel.state.collectAsState()
    var search by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun submitCustom() {
        viewModel.addCustomItem(customName)
        customName = ""
    }

    val query = search.trim().lowercase()
    val visibleFrequent = if (query.isEmpty()) state.frequentItems
    else state.frequentItems.filter { it.contains(query) }
    // Sections that will actually render (non-empty after the search filter).
    val visibleSections = state.sections.mapNotNull { section ->
        val visible = if (query.isEmpty()) section.itemNames else section.itemNames.filter { it.contains(query) }
        if (visible.isNotEmpty()) section to visible else null
    }

    // C1: flat LazyColumn index of a category's header, so tapping a category chip scrolls to it
    // (rather than jumping to the top). Mirrors the exact render order below.
    fun indexOfCategory(categoryId: String): Int {
        var idx = 0
        if (visibleFrequent.isNotEmpty()) idx += 1 + visibleFrequent.size
        if (state.customItems.isNotEmpty()) idx += 1 + state.customItems.size
        for ((section, visible) in visibleSections) {
            if (section.category.categoryId == categoryId) return idx
            idx += 1 + visible.size
        }
        return idx
    }

    // Arrived from the Categories tab with a target category → scroll to it once the catalog loads.
    var didAutoScroll by remember { mutableStateOf(false) }
    LaunchedEffect(initialCategoryId, visibleSections.size) {
        if (!didAutoScroll && initialCategoryId != null &&
            visibleSections.any { it.first.category.categoryId == initialCategoryId }) {
            listState.animateScrollToItem(indexOfCategory(initialCategoryId))
            didAutoScroll = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Pick your items", style = MaterialTheme.typography.headlineSmall)
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search items") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // C1: category quick-jump — tapping scrolls the list to that category's items.
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.loadingCatalog) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            // Previously-added items, ranked by frequency — pinned above every other section.
            if (visibleFrequent.isNotEmpty()) {
                item(key = "frequent-header") {
                    CategoryHeader("⭐", "Frequently Added")
                }
                items(visibleFrequent, key = { "frequent-$it" }) { name ->
                    SelectableItemRow(
                        name = name,
                        checked = name in state.selected,
                        onToggle = { viewModel.toggleItem(name) }
                    )
                }
            }
            // Custom (off-catalog) entries the user typed, shown on top while selected.
            if (state.customItems.isNotEmpty()) {
                item(key = "custom-header") {
                    CategoryHeader("✏️", "Your items")
                }
                items(state.customItems, key = { "custom-$it" }) { name ->
                    SelectableItemRow(
                        name = name,
                        checked = name in state.selected,
                        onToggle = { viewModel.toggleItem(name) }
                    )
                }
            }
            state.sections.forEach { section ->
                val visible = if (query.isEmpty()) section.itemNames
                else section.itemNames.filter { it.contains(query) }
                if (visible.isNotEmpty()) {
                    item(key = "header-${section.category.categoryId}") {
                        CategoryHeader(section.category.emoji, section.category.name)
                    }
                    items(visible, key = { "${section.category.categoryId}-$it" }) { name ->
                        SelectableItemRow(
                            name = name,
                            checked = name in state.selected,
                            onToggle = { viewModel.toggleItem(name) },
                            categoryId = section.category.categoryId
                        )
                    }
                }
            }
            item(key = "custom-input") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Add another item…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { submitCustom() }, enabled = customName.isNotBlank()) {
                        Icon(Icons.Default.Add, "Add custom item")
                    }
                }
            }
        }

        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${state.selected.size} selected", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { viewModel.proceedToName() }, enabled = state.selected.isNotEmpty()) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun CategoryHeader(emoji: String, name: String) {
    Text(
        "$emoji  $name",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SelectableItemRow(name: String, checked: Boolean, onToggle: () -> Unit, categoryId: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        com.shoppilist.shared.ui.components.ItemIcon(name = name, categoryId = categoryId, size = 34.dp)
        Text(
            name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun NameListStep(viewModel: CreateListViewModel) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.backToItems() }) {
                Icon(Icons.Default.ArrowBack, "Back to items")
            }
            Text("Name your list", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "${state.selected.size} items will be added",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("List Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Color tag", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            PRESET_LIST_COLORS.forEach { hex ->
                val color = colorFromHex(hex) ?: Color.Gray
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selectedColor == hex) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape
                        )
                        .clickable { selectedColor = if (selectedColor == hex) null else hex }
                )
            }
        }
        state.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.create(name, description.trim().ifBlank { null }, selectedColor) },
            enabled = !state.creating,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(if (state.creating) "Creating…" else "Create")
        }
    }
}
