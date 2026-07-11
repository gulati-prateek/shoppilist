package com.shoppilist.shared.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.shoppilist.shared.domain.CountryLanguageData
import com.shoppilist.shared.presentation.StartDestination
import com.shoppilist.shared.ui.screens.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object CountrySelection : Screen("country_selection")
    object LanguageSelection : Screen("language_selection/{countryCode}") {
        fun createRoute(countryCode: String) = "language_selection/$countryCode"
    }
    object Login : Screen("login")
    object Register : Screen("register")
    object ProfileSetup : Screen("profile_setup")
    object Home : Screen("home")
    object CreateList : Screen("create_list?categoryId={categoryId}") {
        // Optional categoryId → the pick-items list auto-scrolls to that category on open.
        fun createRoute(categoryId: String? = null) =
            if (categoryId != null) "create_list?categoryId=$categoryId" else "create_list"
    }
    object ListDetail : Screen("list_detail/{listId}") {
        fun createRoute(listId: String) = "list_detail/$listId"
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
    object Activity : Screen("activity/{listId}") {
        fun createRoute(listId: String) = "activity/$listId"
    }
    object Profile : Screen("profile")
    object AdminDashboard : Screen("admin_dashboard")
}

@Composable
fun AppNavigation(startScreen: String = Screen.Splash.route) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startScreen,
        // Keep all screens clear of the system bars: targetSdk 35+ forces edge-to-edge on
        // Android 15+, and the bare-Column screens (onboarding, auth, voice, settings) have
        // no inset handling of their own. Insets are consumed here, so Scaffold-based
        // screens don't double-pad.
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        composable(Screen.Splash.route) {
            SplashScreen { destination ->
                val route = when (destination) {
                    StartDestination.HOME -> Screen.Home.route
                    StartDestination.PROFILE_SETUP -> Screen.ProfileSetup.route
                    // Streamlined: the branded Login screen is the sole not-signed-in entry point.
                    StartDestination.LOGIN, StartDestination.ONBOARDING -> Screen.Login.route
                }
                navController.navigate(route) { popUpTo(0) }
            }
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onCreateAccount = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { needsProfile ->
                    val next = if (needsProfile) Screen.ProfileSetup.route else Screen.Home.route
                    navController.navigate(next) { popUpTo(0) }
                },
                onAdminLogin = { navController.navigate(Screen.AdminDashboard.route) { popUpTo(0) } }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onLogin = { navController.popBackStack() },
                onRegisterSuccess = { needsProfile ->
                    val next = if (needsProfile) Screen.ProfileSetup.route else Screen.Home.route
                    navController.navigate(next) { popUpTo(0) }
                }
            )
        }
        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(
                onDone = { navController.navigate(Screen.Home.route) { popUpTo(0) } }
            )
        }
        composable(Screen.Home.route) {
            MainShell(
                onCreateList = { categoryId -> navController.navigate(Screen.CreateList.createRoute(categoryId)) },
                onOpenList = { listId -> navController.navigate(Screen.ListDetail.createRoute(listId)) },
                onOpenVoice = { navController.navigate(Screen.Voice.route) },
                onOpenAdmin = { navController.navigate(Screen.AdminDashboard.route) },
                onLoggedOut = { navController.navigate(Screen.Login.route) { popUpTo(0) } }
            )
        }
        composable(
            Screen.CreateList.route,
            arguments = listOf(navArgument("categoryId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.read { getStringOrNull("categoryId") }
            CreateListScreen(
                initialCategoryId = categoryId,
                onBack = { navController.popBackStack() },
                onCreated = { listId ->
                    // Land inside the new list; Home stays beneath it on the back stack.
                    navController.navigate(Screen.ListDetail.createRoute(listId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
        composable(Screen.ListDetail.route) { backStackEntry ->
            val listId = backStackEntry.arguments?.read { getStringOrNull("listId") } ?: ""
            ListDetailScreen(
                listId = listId,
                onOpenItem = { itemId -> navController.navigate(Screen.ItemOrderOnline.createRoute(itemId, listId)) },
                onOrderWholeList = { navController.navigate(Screen.OrderWholeList.createRoute(listId)) },
                onOpenAssignments = { navController.navigate(Screen.Assignments.createRoute(listId)) },
                onOpenActivity = { navController.navigate(Screen.Activity.createRoute(listId)) },
                onInvite = { navController.navigate(Screen.Invite.createRoute(listId)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Voice.route) {
            VoiceScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Assignments.route) { backStackEntry ->
            val listId = backStackEntry.arguments?.read { getStringOrNull("listId") } ?: ""
            AssignmentsScreen(listId = listId, onBack = { navController.popBackStack() })
        }
        composable(Screen.Invite.route) { backStackEntry ->
            val listId = backStackEntry.arguments?.read { getStringOrNull("listId") } ?: ""
            InviteScreen(listId = listId, onBack = { navController.popBackStack() })
        }
        composable(Screen.ItemOrderOnline.route) { backStackEntry ->
            val itemId = backStackEntry.arguments?.read { getStringOrNull("itemId") } ?: ""
            val listId = backStackEntry.arguments?.read { getStringOrNull("listId") } ?: ""
            ItemOrderOnlineScreen(
                itemId = itemId,
                listId = listId,
                onBack = { navController.popBackStack() },
                onOrderWholeList = { navController.navigate(Screen.OrderWholeList.createRoute(listId)) }
            )
        }
        composable(Screen.OrderWholeList.route) { backStackEntry ->
            val listId = backStackEntry.arguments?.read { getStringOrNull("listId") } ?: ""
            OrderWholeListScreen(listId = listId, onBack = { navController.popBackStack() })
        }
        composable(Screen.Activity.route) { backStackEntry ->
            val listId = backStackEntry.arguments?.read { getStringOrNull("listId") } ?: ""
            ActivityScreen(listId = listId, onBack = { navController.popBackStack() })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onOpenAdmin = { navController.navigate(Screen.AdminDashboard.route) },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) }
                }
            )
        }
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(onBack = { navController.popBackStack() })
        }
    }
}

