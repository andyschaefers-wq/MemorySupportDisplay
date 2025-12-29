package com.otheruncle.memorydisplay

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.otheruncle.memorydisplay.data.auth.BiometricHelper
import com.otheruncle.memorydisplay.data.auth.BiometricResult
import com.otheruncle.memorydisplay.data.push.NotificationPermissionHelper
import com.otheruncle.memorydisplay.ui.navigation.*
import com.otheruncle.memorydisplay.ui.theme.MemorySupportTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var biometricHelper: BiometricHelper
    
    @Inject
    lateinit var notificationPermissionHelper: NotificationPermissionHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemorySupportTheme {
                MainApp(
                    activity = this,
                    biometricHelper = biometricHelper,
                    notificationPermissionHelper = notificationPermissionHelper
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    activity: FragmentActivity,
    biometricHelper: BiometricHelper,
    notificationPermissionHelper: NotificationPermissionHelper,
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val uiState by viewModel.uiState.collectAsState()

    // Track biometric attempts
    var biometricAttempts by remember { mutableIntStateOf(0) }
    val maxBiometricAttempts = 5
    
    // Notification permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result - we don't need to do anything specific here
        // The user can still use the app without notifications
    }
    
    // Request notification permission when user logs in (Android 13+)
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn && notificationPermissionHelper.shouldRequestPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Show biometric prompt when required
    LaunchedEffect(uiState.requiresBiometric) {
        if (uiState.requiresBiometric && biometricHelper.isBiometricAvailable()) {
            biometricHelper.showBiometricPrompt(
                activity = activity,
                title = "Memory Support Display",
                subtitle = "Use your fingerprint to unlock",
                negativeButtonText = "Use Password",
                maxAttempts = maxBiometricAttempts
            ) { result ->
                when (result) {
                    is BiometricResult.Success -> {
                        biometricAttempts = 0
                        viewModel.onBiometricSuccess()
                    }
                    is BiometricResult.Cancelled -> {
                        // User chose to use password
                        viewModel.onBiometricFailed()
                    }
                    is BiometricResult.Failed -> {
                        biometricAttempts++
                        if (biometricAttempts >= maxBiometricAttempts) {
                            viewModel.onBiometricFailed()
                        }
                        // Otherwise the prompt stays open for retry
                    }
                    is BiometricResult.TooManyAttempts -> {
                        viewModel.onBiometricFailed()
                    }
                    is BiometricResult.Error -> {
                        viewModel.onBiometricFailed()
                    }
                }
            }
        }
    }

    // Session expired dialog
    if (uiState.showSessionExpiredDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismissing without button */ },
            icon = { 
                Icon(
                    Icons.Default.Warning, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                ) 
            },
            title = { Text("Session Expired") },
            text = { Text("Your session has expired. Please log in again to continue.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onSessionExpiredDialogDismissed()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Log In")
                }
            }
        )
    }

    // Show loading or biometric screen
    if (uiState.isLoading || uiState.biometricAutoLoginInProgress) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                if (uiState.biometricAutoLoginInProgress) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Logging in...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        return
    }

    // Show biometric login error as a snackbar-style message
    uiState.biometricLoginError?.let { error ->
        LaunchedEffect(error) {
            // Clear error after a delay
            kotlinx.coroutines.delay(3000)
            viewModel.clearBiometricLoginError()
        }
    }

    // If biometric is required, show a placeholder screen while prompt is up
    if (uiState.requiresBiometric) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Authenticating...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    // Determine if we should show bottom nav (not on auth screens)
    val showBottomNav = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Create.route,
        Screen.Messages.route,
        Screen.Profile.route,
        Screen.Review.route
    )

    // Build nav items list (include Review tab only for reviewers)
    val navItems = remember(uiState.isCalendarReviewer) {
        if (uiState.isCalendarReviewer) {
            bottomNavItems + reviewNavItem
        } else {
            bottomNavItems
        }
    }

    // Determine start destination
    val startDestination = remember(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) Screen.Home.route else Screen.Login.route
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    navItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                if (item.showBadge && uiState.pendingReviewCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(uiState.pendingReviewCount.toString())
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.title
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                }
                            },
                            label = { Text(item.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            onLoginSuccess = { viewModel.onLoginSuccess() }
        )
    }
}
