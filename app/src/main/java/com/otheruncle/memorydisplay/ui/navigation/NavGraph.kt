package com.otheruncle.memorydisplay.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.otheruncle.memorydisplay.ui.auth.LoginScreen
import com.otheruncle.memorydisplay.ui.auth.SetupScreen
import com.otheruncle.memorydisplay.ui.create.*
import com.otheruncle.memorydisplay.ui.home.HomeScreen
import com.otheruncle.memorydisplay.ui.messages.MessagesScreen
import com.otheruncle.memorydisplay.ui.profile.ChangePasswordScreen
import com.otheruncle.memorydisplay.ui.profile.ProfileScreen
import com.otheruncle.memorydisplay.ui.review.ReviewDetailScreen
import com.otheruncle.memorydisplay.ui.review.ReviewScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ==================== Auth Screens ====================

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    onLoginSuccess()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onForgotPassword = { /* Open browser intent */ }
            )
        }

        composable(
            route = Screen.Setup.route,
            arguments = listOf(
                navArgument("token") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "https://otheruncle.com/memory_display/setup?token={token}"
                },
                navDeepLink {
                    uriPattern = "memorydisplay://setup/{token}"
                }
            )
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            SetupScreen(
                token = token,
                onSetupComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ==================== Main Screens ====================

        composable(Screen.Home.route) {
            HomeScreen(
                onCardClick = { cardId ->
                    navController.navigate(Screen.CardDetail.createRoute(cardId))
                },
                onEditCard = { cardId ->
                    navController.navigate(Screen.EditCard.createRoute(cardId))
                }
            )
        }

        composable(Screen.Create.route) {
            CreateScreen(
                onSelectType = { cardType ->
                    when (cardType) {
                        "professional" -> navController.navigate(Screen.CreateProfessional.route)
                        "family-event" -> navController.navigate(Screen.CreateFamilyEvent.route)
                        "other-event" -> navController.navigate(Screen.CreateOtherEvent.route)
                        "trip" -> navController.navigate(Screen.CreateTrip.route)
                        "reminder" -> navController.navigate(Screen.CreateReminder.route)
                    }
                }
            )
        }

        composable(Screen.Messages.route) {
            MessagesScreen(
                onMessageSent = {
                    // Navigate back to Home after sending message
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onChangePassword = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Review.route) {
            ReviewScreen(
                onEventClick = { eventId ->
                    navController.navigate(Screen.ReviewDetail.createRoute(eventId))
                }
            )
        }

        // ==================== Create Form Screens ====================

        composable(Screen.CreateProfessional.route) {
            ProfessionalAppointmentFormScreen(
                onSave = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateFamilyEvent.route) {
            FamilyEventFormScreen(
                onSave = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateOtherEvent.route) {
            OtherEventFormScreen(
                onSave = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateTrip.route) {
            TripFormScreen(
                onSave = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateReminder.route) {
            ReminderFormScreen(
                onSave = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        // ==================== Detail/Edit Screens ====================

        composable(
            route = Screen.CardDetail.route,
            arguments = listOf(navArgument("cardId") { type = NavType.IntType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getInt("cardId") ?: return@composable
            CardDetailScreen(
                cardId = cardId,
                onEdit = { navController.navigate(Screen.EditCard.createRoute(cardId)) },
                onBack = { navController.popBackStack() }
            )
        }

        // Edit router - determines card type and redirects to appropriate form
        composable(
            route = Screen.EditCard.route,
            arguments = listOf(navArgument("cardId") { type = NavType.IntType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getInt("cardId") ?: return@composable
            EditCardRouter(
                cardId = cardId,
                navController = navController
            )
        }

        // Type-specific edit screens
        composable(
            route = Screen.EditProfessional.route,
            arguments = listOf(navArgument("cardId") { type = NavType.IntType })
        ) {
            ProfessionalAppointmentFormScreen(
                onSave = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditFamilyEvent.route,
            arguments = listOf(navArgument("cardId") { type = NavType.IntType })
        ) {
            FamilyEventFormScreen(
                onSave = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditOtherEvent.route,
            arguments = listOf(navArgument("cardId") { type = NavType.IntType })
        ) {
            OtherEventFormScreen(
                onSave = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditTrip.route,
            arguments = listOf(navArgument("cardId") { type = NavType.IntType })
        ) {
            TripFormScreen(
                onSave = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditReminder.route,
            arguments = listOf(navArgument("cardId") { type = NavType.IntType })
        ) {
            ReminderFormScreen(
                onSave = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditMessage.route,
            arguments = listOf(navArgument("cardId") { type = NavType.IntType })
        ) {
            MessagesScreen(
                onMessageSent = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ReviewDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.IntType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getInt("eventId") ?: return@composable
            ReviewDetailScreen(
                eventId = eventId,
                onSave = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onSuccess = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
