package com.shoppilist.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.shoppilist.shared.data.local.SponsoredRetailerEntity
import com.shoppilist.shared.presentation.OrderOnlineViewModel
import kotlinx.coroutines.launch

private val SponsoredAmber = Color(0xFFF59E0B)

@Composable
private fun RetailerTile(
    retailer: SponsoredRetailerEntity,
    onClick: () -> Unit
) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            // Brand-colored monogram tile (we don't bundle trademarked logos).
            com.shoppilist.shared.ui.components.VendorIcon(name = retailer.name, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    retailer.name,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (retailer.isSponsored) {
                    Text(
                        "Sponsored",
                        style = MaterialTheme.typography.labelSmall,
                        color = SponsoredAmber
                    )
                }
            }
        }
    }
}

/** Per-item "Order Online" sheet (§2.13): sponsored tiles are always visually distinct, organic ones always shown alongside. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemOrderOnlineScreen(
    itemId: String,
    listId: String? = null,
    viewModel: OrderOnlineViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onOrderWholeList: () -> Unit = {}
) {
    LaunchedEffect(itemId) { viewModel.loadForItem(itemId) }
    val item by viewModel.item.collectAsState()
    val retailers by viewModel.retailers.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.name ?: "Order Online") },
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
            item?.let { i ->
                Text("Qty: ${i.quantity} ${i.unit ?: ""}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
            }
            Text("Order Online", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (retailers.isEmpty()) {
                Text("No retailers available for your country yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyVerticalGrid(
                    // Adaptive instead of Fixed(2): scales from small phones to tablets/foldables.
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    // Weighted so the "Order Whole List" button below always stays on screen.
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(retailers) { retailer ->
                        RetailerTile(retailer) {
                            val name = item?.name ?: ""
                            viewModel.logClick(retailer, itemId, listId, "ITEM")
                            uriHandler.openUri(viewModel.buildSearchUrl(retailer, name))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onOrderWholeList, modifier = Modifier.fillMaxWidth()) {
                Text("Order Whole List Online →")
            }
        }
    }
}

/** "Order Whole List" (§2.13): none of the seeded retailers support a real basket API, so we open
 *  the first item's search and list the rest for the user to add manually. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderWholeListScreen(
    listId: String,
    viewModel: OrderOnlineViewModel = koinViewModel(),
    onBack: () -> Unit = {}
) {
    val retailers by viewModel.retailers.collectAsState()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var manualChecklist by remember { mutableStateOf<List<String>>(emptyList()) }
    var pickedRetailer by remember { mutableStateOf<SponsoredRetailerEntity?>(null) }

    LaunchedEffect(listId) { viewModel.loadRetailersOnly() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order All Online") },
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
            Text("Choose a retailer", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(retailers) { retailer ->
                    RetailerTile(retailer) {
                        pickedRetailer = retailer
                        scope.launch {
                            val names = viewModel.getItemNamesForList(listId)
                            val plan = viewModel.buildWholeListPlan(retailer, names)
                            viewModel.logClick(retailer, null, listId, "WHOLE_LIST")
                            plan.urlToOpen?.let { uriHandler.openUri(it) }
                            manualChecklist = plan.remainingItemsToAddManually
                        }
                    }
                }
            }

            if (pickedRetailer != null) {
                Spacer(Modifier.height(24.dp))
                Text("Add these manually in ${pickedRetailer?.name}:", style = MaterialTheme.typography.titleMedium)
                manualChecklist.forEach { name -> Text("• $name", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
