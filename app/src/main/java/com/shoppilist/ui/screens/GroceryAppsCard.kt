package com.shoppilist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.shoppilist.shared.data.local.GroceryAppEntity

/** Dismissible local grocery-app recommendations card (§2.10). */
@Composable
fun GroceryAppsCard(apps: List<GroceryAppEntity>, onDismiss: () -> Unit) {
    if (apps.isEmpty()) return
    val uriHandler = LocalUriHandler.current

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Popular grocery apps near you", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Dismiss") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                apps.forEach { app ->
                    AssistChip(
                        onClick = { uriHandler.openUri(app.storeUrl) },
                        label = { Text("${app.logoEmoji} ${app.name}") }
                    )
                }
            }
        }
    }
}
