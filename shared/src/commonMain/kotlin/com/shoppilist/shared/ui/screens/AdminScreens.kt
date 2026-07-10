@file:OptIn(ExperimentalMaterial3Api::class)

package com.shoppilist.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shoppilist.shared.backend.CustomItemReport
import com.shoppilist.shared.domain.CatalogRegion
import com.shoppilist.shared.presentation.AdminViewModel
import org.koin.compose.viewmodel.koinViewModel

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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Admin — item review", style = MaterialTheme.typography.headlineSmall)
        }

        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        state.info?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (!state.loading && state.reports.isEmpty() && state.error == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No pending items — all caught up 🎉",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                items(state.reports, key = { it.id }) { report ->
                    ListItem(
                        headlineContent = {
                            Text(report.name.replaceFirstChar { it.uppercase() })
                        },
                        supportingContent = {
                            val meta = listOfNotNull(
                                report.reportedByName?.let { "by $it" },
                                report.countryCode
                            ).joinToString(" · ")
                            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall)
                        },
                        trailingContent = {
                            Row {
                                TextButton(onClick = { reviewing = report }) { Text("Review") }
                                TextButton(onClick = { viewModel.reject(report) }) {
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
