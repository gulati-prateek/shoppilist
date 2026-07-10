@file:OptIn(ExperimentalMaterial3Api::class)

package com.shoppilist.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.shoppilist.shared.data.local.ShoppingListEntity
import com.shoppilist.shared.location.rememberLocationController
import com.shoppilist.shared.presentation.CreateListStep
import com.shoppilist.shared.presentation.CreateListViewModel
import com.shoppilist.shared.presentation.HomeViewModel
import com.shoppilist.shared.presentation.ListMeta

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
    onCreateList: () -> Unit = {},
    onOpenList: (String) -> Unit = {},
    onOpenVoice: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val listMeta by viewModel.listMeta.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ShoppiList") },
                actions = {
                    IconButton(onClick = onOpenVoice) {
                        Icon(Icons.Default.Mic, "Voice")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateList) {
                Icon(Icons.Default.Add, "Create List")
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
            LocationChipRow(viewModel)
            LazyColumn {
                items(state.lists, key = { it.listId }) { list ->
                    HomeListRow(
                        list = list,
                        meta = listMeta[list.listId],
                        onOpen = { onOpenList(list.listId) },
                        onArchive = { viewModel.archiveList(list.listId) },
                        onTogglePin = { viewModel.togglePin(list.listId, !list.pinned) },
                        onDelete = { viewModel.deleteList(list.listId) }
                    )
                    HorizontalDivider()
                }
            }
            if (state.error != null) {
                Text("Error: ${state.error}", modifier = Modifier.padding(16.dp))
            }
        }
    }
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
private fun HomeListRow(
    list: ShoppingListEntity,
    meta: ListMeta?,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
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
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onArchive()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Archive,
                    contentDescription = "Archive list",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        val memberCount = meta?.memberCount ?: 0
        val peopleLabel = if (memberCount <= 1) "Just you" else "$memberCount people"
        ListItem(
            headlineContent = { Text(list.name) },
            supportingContent = {
                Column {
                    val description = list.description
                    if (!description.isNullOrBlank()) Text(description)
                    Text("${meta?.itemCount ?: 0} items · $peopleLabel", style = MaterialTheme.typography.bodySmall)
                }
            },
            leadingContent = {
                val color = colorFromHex(list.colorHex)
                if (color != null) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            },
            trailingContent = {
                Row {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            if (list.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (list.pinned) "Unpin" else "Pin to top",
                            tint = if (list.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete list",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen() }
        )
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
    onBack: () -> Unit,
    onCreated: (listId: String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.createdListId) {
        state.createdListId?.let(onCreated)
    }

    when (state.step) {
        CreateListStep.PICK_ITEMS -> PickItemsStep(viewModel, onBack)
        CreateListStep.NAME -> NameListStep(viewModel)
    }
}

@Composable
private fun PickItemsStep(viewModel: CreateListViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var search by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }

    fun submitCustom() {
        viewModel.addCustomItem(customName)
        customName = ""
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

        if (state.loadingCatalog) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        val query = search.trim().lowercase()
        LazyColumn(modifier = Modifier.weight(1f)) {
            // Previously-added items, ranked by frequency — pinned above every other section.
            val visibleFrequent = if (query.isEmpty()) state.frequentItems
            else state.frequentItems.filter { it.contains(query) }
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
                            onToggle = { viewModel.toggleItem(name) }
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
private fun SelectableItemRow(name: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 4.dp)
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
