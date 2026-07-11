@file:OptIn(ExperimentalMaterial3Api::class)

package com.shoppilist.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shoppilist.shared.currentTimeMillis
import com.shoppilist.shared.data.local.ListActivityAction
import com.shoppilist.shared.data.local.ListActivityEntity
import com.shoppilist.shared.presentation.ActivityViewModel
import com.shoppilist.shared.ui.components.ProfileAvatar
import org.koin.compose.viewmodel.koinViewModel

/** Item 11: per-list activity feed — who did what, newest first. */
@Composable
fun ActivityScreen(
    listId: String,
    onBack: () -> Unit,
    viewModel: ActivityViewModel = koinViewModel()
) {
    LaunchedEffect(listId) { viewModel.load(listId) }
    val activity by viewModel.activity.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (activity.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No activity yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(activity, key = { it.id }) { entry ->
                    ActivityRow(entry)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(entry: ListActivityEntity) {
    ListItem(
        leadingContent = {
            ProfileAvatar(
                initial = entry.actorName.firstOrNull()?.toString(),
                seed = entry.actorUserId,
                size = 36.dp
            )
        },
        headlineContent = { Text(describeActivity(entry)) },
        supportingContent = { Text(relativeTime(entry.createdAt), style = MaterialTheme.typography.bodySmall) }
    )
}

private fun describeActivity(entry: ListActivityEntity): String {
    val who = entry.actorName
    val item = entry.itemName ?: "an item"
    return when (entry.action) {
        ListActivityAction.ADDED_ITEM -> "$who added $item"
        ListActivityAction.DELETED_ITEM -> "$who removed $item"
        ListActivityAction.EDITED_ITEM -> "$who edited $item"
        ListActivityAction.CHECKED_ITEM -> "$who checked off $item"
        ListActivityAction.UNCHECKED_ITEM -> "$who un-checked $item"
        ListActivityAction.RENAMED_LIST -> "$who renamed the list${entry.detail?.let { " to \"$it\"" } ?: ""}"
        ListActivityAction.MEMBER_INVITED -> "$who invited ${entry.detail ?: "someone"}"
        ListActivityAction.MEMBER_JOINED -> "$who joined the list"
        ListActivityAction.MEMBER_REMOVED -> "$who removed ${entry.detail ?: "a member"}"
    }
}

private fun relativeTime(then: Long): String {
    val diff = currentTimeMillis() - then
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
