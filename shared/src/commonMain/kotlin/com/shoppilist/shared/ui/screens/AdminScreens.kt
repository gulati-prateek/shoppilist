@file:OptIn(ExperimentalMaterial3Api::class)

package com.shoppilist.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shoppilist.shared.backend.CustomItemReport
import com.shoppilist.shared.data.local.GlobalItemEntity
import com.shoppilist.shared.data.local.SponsoredRetailerEntity
import com.shoppilist.shared.domain.CatalogRegion
import com.shoppilist.shared.presentation.AdminViewModel
import org.koin.compose.viewmodel.koinViewModel

private enum class AdminTab(val label: String) { REVIEW("Review"), VENDORS("Vendors"), CATALOG("Catalog") }

/**
 * Admin dashboard v1: the review queue of items users added that aren't in the master catalog
 * (item 9). Approve — optionally after editing name/category/region — publishes into the
 * remote catalog; reject dismisses. Grows gradually per the product direction (item 10).
 */
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.load() }
    val state by viewModel.state.collectAsState()
    var reviewing by remember { mutableStateOf<CustomItemReport?>(null) }

    reviewing?.let { report ->
        ApproveItemDialog(
            report = report,
            categories = state.categories.map { it.categoryId to "${it.emoji} ${it.name}" },
            onApprove = { finalName, categoryId, region ->
                viewModel.approve(report, finalName, categoryId, region)
                reviewing = null
            },
            onDismiss = { reviewing = null }
        )
    }

    var tab by remember { mutableStateOf(AdminTab.REVIEW) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Admin", style = MaterialTheme.typography.headlineSmall)
        }

        if (!state.loading && !state.isAdmin) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "This account doesn't have admin access.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        TabRow(selectedTabIndex = tab.ordinal) {
            AdminTab.entries.forEach { t ->
                Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label) })
            }
        }

        if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        state.info?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp))
        }

        when (tab) {
            AdminTab.REVIEW -> ReviewTab(state, onReview = { reviewing = it }, onReject = { viewModel.reject(it) })
            AdminTab.VENDORS -> VendorsTab(viewModel)
            AdminTab.CATALOG -> CatalogTab(viewModel)
        }
    }
}

@Composable
private fun ReviewTab(
    state: com.shoppilist.shared.presentation.AdminUiState,
    onReview: (CustomItemReport) -> Unit,
    onReject: (CustomItemReport) -> Unit
) {
    if (!state.loading && state.reports.isEmpty() && state.error == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending items — all caught up 🎉",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn {
            items(state.reports, key = { it.id }) { report ->
                ListItem(
                    headlineContent = { Text(report.name.replaceFirstChar { it.uppercase() }) },
                    supportingContent = {
                        val meta = listOfNotNull(report.reportedByName?.let { "by $it" }, report.countryCode).joinToString(" · ")
                        if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)
                    },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { onReview(report) }) { Text("Review") }
                            TextButton(onClick = { onReject(report) }) {
                                Text("Reject", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

/** A1/A2: vendor affiliate links with a per-vendor on/off switch. */
@Composable
private fun VendorsTab(viewModel: AdminViewModel) {
    val retailers by viewModel.retailers.collectAsState()
    if (retailers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No vendors configured", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn {
        items(retailers, key = { it.id }) { r ->
            ListItem(
                leadingContent = { Text(r.logoEmoji, style = MaterialTheme.typography.headlineSmall) },
                headlineContent = { Text(r.name) },
                supportingContent = {
                    val meta = listOfNotNull(
                        r.countryCode,
                        if (r.isSponsored) "Sponsored" else "Organic",
                        r.affiliateProgram
                    ).joinToString(" · ")
                    Text(meta, style = MaterialTheme.typography.bodySmall)
                },
                trailingContent = {
                    Switch(
                        checked = r.isActive,
                        onCheckedChange = { viewModel.setRetailerActive(r.id, it) }
                    )
                }
            )
            HorizontalDivider()
        }
    }
}

/** A4: browse a category and add / rename / remove its catalog items. */
@Composable
private fun CatalogTab(viewModel: AdminViewModel) {
    val state by viewModel.state.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val items by viewModel.categoryItems.collectAsState()
    var categoryExpanded by remember { mutableStateOf(false) }
    var newItem by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<GlobalItemEntity?>(null) }

    editing?.let { item ->
        var editName by remember { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Rename item") },
            text = {
                OutlinedTextField(value = editName, onValueChange = { editName = it }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = { viewModel.updateCatalogItem(item, editName); editing = null },
                    enabled = editName.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val selectedName = state.categories.find { it.categoryId == selectedCategoryId }
            ?.let { "${it.emoji} ${it.name}" } ?: "Select a category"
        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
            OutlinedTextField(
                value = selectedName, onValueChange = {}, readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth().padding(16.dp)
            )
            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                state.categories.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.emoji} ${c.name}") },
                        onClick = { viewModel.selectCategory(c.categoryId); categoryExpanded = false }
                    )
                }
            }
        }

        if (selectedCategoryId != null) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = newItem, onValueChange = { newItem = it },
                    label = { Text("Add item to this category") }, singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.addCatalogItem(newItem, selectedCategoryId!!); newItem = "" },
                    enabled = newItem.isNotBlank()
                ) { Icon(Icons.Default.Add, "Add item") }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items, key = { it.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.name.replaceFirstChar { it.uppercase() }) },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { editing = item }) { Text("Edit") }
                            TextButton(onClick = { viewModel.deleteCatalogItem(item.id) }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ApproveItemDialog(
    report: CustomItemReport,
    categories: List<Pair<String, String>>,
    onApprove: (finalName: String, categoryId: String, region: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(report.name) }
    var categoryId by remember { mutableStateOf(categories.firstOrNull()?.first ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }
    // Default the region to where the reporter is, falling back to India.
    var region by remember {
        mutableStateOf(
            CatalogRegion.forCountry(report.countryCode)
                .takeIf { it in CatalogRegion.REMOTE_REGIONS } ?: CatalogRegion.INDIA
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to master catalog") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.first == categoryId }?.second ?: "Pick a category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { (id, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    categoryId = id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Catalog region", style = MaterialTheme.typography.labelMedium)
                Row {
                    CatalogRegion.REMOTE_REGIONS.forEach { option ->
                        FilterChip(
                            selected = region == option,
                            onClick = { region = option },
                            label = { Text(option) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApprove(name, categoryId, region) },
                enabled = name.isNotBlank() && categoryId.isNotBlank()
            ) { Text("Approve") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
