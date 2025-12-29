package com.otheruncle.memorydisplay.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations
 */
sealed class Screen(val route: String) {
    // Auth screens
    data object Login : Screen("login")
    data object Setup : Screen("setup/{token}") {
        fun createRoute(token: String) = "setup/$token"
    }

    // Main screens (bottom nav)
    data object Home : Screen("home")
    data object Create : Screen("create")
    data object Messages : Screen("messages")
    data object Profile : Screen("profile")
    data object Review : Screen("review")

    // Detail/Form screens
    data object CardDetail : Screen("card/{cardId}") {
        fun createRoute(cardId: Int) = "card/$cardId"
    }

    data object CreateProfessional : Screen("create/professional")
    data object CreateFamilyEvent : Screen("create/family-event")
    data object CreateOtherEvent : Screen("create/other-event")
    data object CreateTrip : Screen("create/trip")
    data object CreateReminder : Screen("create/reminder")

    data object EditCard : Screen("edit/{cardId}") {
        fun createRoute(cardId: Int) = "edit/$cardId"
    }

    // Type-specific edit routes
    data object EditProfessional : Screen("edit/professional/{cardId}") {
        fun createRoute(cardId: Int) = "edit/professional/$cardId"
    }
    data object EditFamilyEvent : Screen("edit/family-event/{cardId}") {
        fun createRoute(cardId: Int) = "edit/family-event/$cardId"
    }
    data object EditOtherEvent : Screen("edit/other-event/{cardId}") {
        fun createRoute(cardId: Int) = "edit/other-event/$cardId"
    }
    data object EditTrip : Screen("edit/trip/{cardId}") {
        fun createRoute(cardId: Int) = "edit/trip/$cardId"
    }
    data object EditReminder : Screen("edit/reminder/{cardId}") {
        fun createRoute(cardId: Int) = "edit/reminder/$cardId"
    }
    data object EditMessage : Screen("edit/message/{cardId}") {
        fun createRoute(cardId: Int) = "edit/message/$cardId"
    }

    data object ReviewDetail : Screen("review/{eventId}") {
        fun createRoute(eventId: Int) = "review/$eventId"
    }

    data object ChangePassword : Screen("profile/password")
}

/**
 * Bottom navigation items
 */
data class BottomNavItem(
    val screen: Screen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showBadge: Boolean = false
)

val bottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.Home,
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        screen = Screen.Create,
        title = "Create",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircle
    ),
    BottomNavItem(
        screen = Screen.Messages,
        title = "Messages",
        selectedIcon = Icons.Filled.Email,
        unselectedIcon = Icons.Outlined.Email
    ),
    BottomNavItem(
        screen = Screen.Profile,
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)

// Review tab is only shown for calendar reviewers
val reviewNavItem = BottomNavItem(
    screen = Screen.Review,
    title = "Review",
    selectedIcon = Icons.Filled.Checklist,
    unselectedIcon = Icons.Outlined.Checklist,
    showBadge = true
)
