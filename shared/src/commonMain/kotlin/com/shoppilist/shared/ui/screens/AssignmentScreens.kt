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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.shoppilist.shared.resources.*
import com.shoppilist.shared.presentation.AssignmentsViewModel

/** "Who's Getting What" panel (§2.11). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    listId: String,
    viewModel: AssignmentsViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    LaunchedEffect(listId) { viewModel.load(listId) }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_whos_getting_what)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            items(state.byAssignee) { summary ->
                val name = state.userNames[summary.userId] ?: "Member"
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${summary.items.size} items · ${summary.checkedCount}✓",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        summary.items.joinToString(" · ") { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Divider()
            }

            if (state.unassigned.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(stringResource(Res.string.label_unassigned), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.unassigned.size} items",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.unassigned.forEach { item ->
                            Text(
                                item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }

            if (state.byAssignee.isEmpty() && state.unassigned.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.empty_no_items),
                        modifier = Modifier.padding(vertical = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
