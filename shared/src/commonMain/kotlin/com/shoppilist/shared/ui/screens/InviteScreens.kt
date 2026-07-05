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
import androidx.compose.ui.platform.LocalClipboardManager
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.shoppilist.shared.resources.*
import com.shoppilist.shared.data.local.ListRole
import com.shoppilist.shared.presentation.InviteViewModel

@Composable
private fun roleLabel(role: ListRole): String = when (role) {
    ListRole.OWNER -> stringResource(Res.string.role_owner)
    ListRole.EDITOR -> stringResource(Res.string.role_editor)
    ListRole.VIEWER -> stringResource(Res.string.role_viewer)
}

/** Invite / member management screen (§2.5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteScreen(
    listId: String,
    viewModel: InviteViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    LaunchedEffect(listId) { viewModel.loadMembers(listId) }
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current

    var contact by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf("email") }
    var roleExpanded by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf(ListRole.EDITOR) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_invite)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = contact,
                onValueChange = {
                    contact = it
                    channel = if (it.contains("@")) "email" else "phone"
                },
                label = { Text(stringResource(Res.string.label_email_or_phone)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                OutlinedTextField(
                    value = roleLabel(role),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.label_role)) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                    ListRole.entries.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(roleLabel(r)) },
                            onClick = { role = r; roleExpanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.sendInvite(listId, contact, channel, role) },
                enabled = contact.isNotBlank(),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(Res.string.action_send_invite))
            }

            state.lastInviteLink?.let { link ->
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Or share link: $link", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { clipboard.setText(AnnotatedString(link)) }) { Text(stringResource(Res.string.action_copy)) }
                }
            }
            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(Res.string.title_members), style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(state.members) { member ->
                    ListItem(
                        headlineContent = { Text(state.memberNames[member.userId] ?: member.userId) },
                        supportingContent = { Text(roleLabel(member.role)) },
                        trailingContent = {
                            if (member.role != ListRole.OWNER) {
                                TextButton(onClick = { viewModel.removeMember(listId, member.userId) }) { Text(stringResource(Res.string.action_remove)) }
                            }
                        }
                    )
                }
            }
        }
    }
}
