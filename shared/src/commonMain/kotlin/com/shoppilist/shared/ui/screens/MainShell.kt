@file:OptIn(ExperimentalMaterial3Api::class)

package com.shoppilist.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shoppilist.shared.presentation.CategoriesViewModel
import org.koin.compose.viewmodel.koinViewModel

private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    CATEGORIES("Categories", Icons.Default.GridView),
    LISTS("Lists", Icons.Default.ListAlt),
    PROFILE("Profile", Icons.Default.Person)
}

/**
 * The authenticated app shell (from the design mockup): a persistent bottom navigation bar with a
 * central gold "+" FAB. Deeper screens (list detail, create list, activity, …) are pushed onto the
 * outer nav controller and cover the shell.
 */
@Composable
fun MainShell(
    onCreateList: () -> Unit,
    onOpenList: (String) -> Unit,
    onOpenVoice: () -> Unit,
    onOpenAdmin: () -> Unit,
    onLoggedOut: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.HOME,
                    onClick = { tab = MainTab.HOME },
                    icon = { Icon(MainTab.HOME.icon, null) },
                    label = { Text(MainTab.HOME.label) }
                )
                NavigationBarItem(
                    selected = tab == MainTab.CATEGORIES,
                    onClick = { tab = MainTab.CATEGORIES },
                    icon = { Icon(MainTab.CATEGORIES.icon, null) },
                    label = { Text(MainTab.CATEGORIES.label) }
                )
                // Spacer slot so the two right tabs clear the centered FAB.
                Spacer(Modifier.weight(1f))
                NavigationBarItem(
                    selected = tab == MainTab.LISTS,
                    onClick = { tab = MainTab.LISTS },
                    icon = { Icon(MainTab.LISTS.icon, null) },
                    label = { Text(MainTab.LISTS.label) }
                )
                NavigationBarItem(
                    selected = tab == MainTab.PROFILE,
                    onClick = { tab = MainTab.PROFILE },
                    icon = { Icon(MainTab.PROFILE.icon, null) },
                    label = { Text(MainTab.PROFILE.label) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateList,
                containerColor = MaterialTheme.colorScheme.tertiary,   // gold
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Icon(Icons.Default.Add, "New list")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                MainTab.HOME -> HomeScreen(
                    onCreateList = onCreateList,
                    onOpenList = onOpenList,
                    onOpenVoice = onOpenVoice,
                    onOpenProfile = { tab = MainTab.PROFILE },
                    showFab = false
                )
                MainTab.CATEGORIES -> CategoriesScreen(onStartList = onCreateList)
                MainTab.LISTS -> HomeScreen(
                    onCreateList = onCreateList,
                    onOpenList = onOpenList,
                    onOpenVoice = onOpenVoice,
                    onOpenProfile = { tab = MainTab.PROFILE },
                    showFab = false
                )
                MainTab.PROFILE -> ProfileScreen(
                    onBack = { tab = MainTab.HOME },
                    onOpenAdmin = onOpenAdmin,
                    onLoggedOut = onLoggedOut
                )
            }
        }
    }
}

/** Categories tab: browse the catalog taxonomy; tapping a category starts a new list. */
@Composable
fun CategoriesScreen(
    onStartList: () -> Unit,
    viewModel: CategoriesViewModel = koinViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Categories") }) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories, key = { it.categoryId }) { category ->
                ElevatedCard(onClick = onStartList) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(category.emoji, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            category.name,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
