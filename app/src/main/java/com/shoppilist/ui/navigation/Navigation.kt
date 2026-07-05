package com.shoppilist.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shoppilist.shared.domain.CountryLanguageData
import com.shoppilist.ui.screens.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object CountrySelection : Screen("country_selection")
    object LanguageSelection : Screen("language_selection/{countryCode}") {
        fun createRoute(countryCode: String) = "language_selection/$countryCode"
    }
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object CreateList : Screen("create_list")
    object ListDetail : Screen("list_detail/{listId}") {
        fun createRoute(listId: String) = "list_detail/$listId"
    }
    object AddItem : Screen("add_item/{listId}") {
        fun createRoute(listId: String) = "add_item/$listId"
    }
    object Voice : Screen("voice")
    object Assignments : Screen("assignments/{listId}") {
        fun createRoute(listId: String) = "assignments/$listId"
    }
    object Invite : Screen("invite/{listId}") {
        fun createRoute(listId: String) = "invite/$listId"
    }
    object ItemOrderOnline : Screen("order_item/{itemId}/{listId}") {
        fun createRoute(itemId: String, listId: String) = "order_item/$itemId/$listId"
    }
    object OrderWholeList : Screen("order_list/{listId}") {
        fun createRoute(listId: String) = "order_list/$listId"
    }
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(startScreen: String = Screen.Splash.route) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startScreen
    ) {
        composable(Screen.Splash.route) {
            SplashScreen { navController.navigate(Screen.Onboarding.route) }
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen { navController.navigate(Screen.CountrySelection.route) }
        }
        composable(Screen.CountrySelection.route) {
            CountrySelectionScreen { country ->
                navController.navigate(Screen.LanguageSelection.createRoute(country.code))
            }
        }
        composable(Screen.LanguageSelection.route) { backStackEntry ->
            val countryCode = backStackEntry.arguments?.getString("countryCode") ?: "US"
            val country = CountryLanguageData.countries.find { it.code == countryCode }
                ?: CountryLanguageData.countries.first()
            LanguageSelectionScreen(country = country) {
                navController.navigate(Screen.Login.route)
            }
        }
        composable(Screen.Login.route) {
            LoginScreen { navController.navigate(Screen.Home.route) { popUpTo(0) } }
        }
        composable(Screen.Register.route) {
            RegisterScreen { navController.navigate(Screen.Home.route) { popUpTo(0) } }
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onCreateList = { navController.navigate(Screen.CreateList.route) },
                onOpenList = { listId -> navController.navigate(Screen.ListDetail.createRoute(listId)) },
                onOpenVoice = { navController.navigate(Screen.Voice.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.CreateList.route) {
            CreateListScreen { navController.popBackStack() }
        }
        composable(Screen.ListDetail.route) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: ""
            ListDetailScreen(
                listId = listId,
                onOpenItem = { itemId -> navController.navigate(Screen.ItemOrderOnline.createRoute(itemId, listId)) },
                onOrderWholeList = { navController.navigate(Screen.OrderWholeList.createRoute(listId)) },
                onOpenAssignments = { navController.navigate(Screen.Assignments.createRoute(listId)) },
                onInvite = { navController.navigate(Screen.Invite.createRoute(listId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddItem.route) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: ""
            AddItemScreen(listId = listId, onBack = { navController.popBackStack() })
        }
        composable(Screen.Voice.route) {
            VoiceScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Assignments.route) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: ""
            AssignmentsScreen(listId = listId, onBack = { navController.popBackStack() })
        }
        composable(Screen.Invite.route) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: ""
            InviteScreen(listId = listId, onBack = { navController.popBackStack() })
        }
        composable(Screen.ItemOrderOnline.route) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val listId = backStackEntry.arguments?.getString("listId") ?: ""
            ItemOrderOnlineScreen(
                itemId = itemId,
                listId = listId,
                onBack = { navController.popBackStack() },
                onOrderWholeList = { navController.navigate(Screen.OrderWholeList.createRoute(listId)) }
            )
        }
        composable(Screen.OrderWholeList.route) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: ""
            OrderWholeListScreen(listId = listId, onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

