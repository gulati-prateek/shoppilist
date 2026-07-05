@file:OptIn(ExperimentalMaterial3Api::class)

package com.shoppilist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
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
import org.koin.androidx.compose.koinViewModel
import com.shoppilist.shared.data.local.ShoppingListEntity
import com.shoppilist.shared.presentation.HomeViewModel
import com.shoppilist.shared.presentation.ListMeta

private val PRESET_LIST_COLORS = listOf("#2ECC71", "#3B82F6", "#F59E0B", "#EF4444", "#8B5CF6", "#6B7280")

private fun colorFromHex(hex: String?): Color? = try {
    hex?.takeIf { it.isNotBlank() }?.let { Color(android.graphics.Color.parseColor(it)) }
} catch (e: Exception) {
    null
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
            LazyColumn {
                items(state.lists, key = { it.listId }) { list ->
                    HomeListRow(
                        list = list,
                        meta = listMeta[list.listId],
                        onOpen = { onOpenList(list.listId) },
                        onArchive = { viewModel.archiveList(list.listId) },
                        onTogglePin = { viewModel.togglePin(list.listId, !list.pinned) }
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

@Composable
private fun HomeListRow(
    list: ShoppingListEntity,
    meta: ListMeta?,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onTogglePin: () -> Unit
) {
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
                IconButton(onClick = onTogglePin) {
                    Icon(
                        if (list.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (list.pinned) "Unpin" else "Pin to top",
                        tint = if (list.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen() }
        )
    }
}

@Composable
fun CreateListScreen(viewModel: HomeViewModel = koinViewModel(), onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Create List", style = MaterialTheme.typography.headlineSmall)
        }
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
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    viewModel.createList(name.trim(), description.trim().ifBlank { null }, selectedColor)
                    onBack()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Create")
        }
    }
}
