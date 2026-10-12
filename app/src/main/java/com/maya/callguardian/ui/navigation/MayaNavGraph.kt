package com.maya.callguardian.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.maya.callguardian.MayaApplication
import com.maya.callguardian.ui.capability.CapabilityScreen
import com.maya.callguardian.ui.dashboard.DashboardScreen
import com.maya.callguardian.ui.history.CallHistoryScreen
import com.maya.callguardian.ui.live_call.LiveCallGuardianScreen
import com.maya.callguardian.ui.settings.SettingsScreen
import com.maya.callguardian.ui.threat_details.ThreatDetailsScreen
import com.maya.callguardian.ui.theme.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object LiveCall : Screen("live_call", "Guardian", Icons.Default.Call)
    object ThreatDetails : Screen("threat_details", "Threats", Icons.Default.Warning)
    object History : Screen("history", "History", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Capabilities : Screen("capabilities", "Capabilities", Icons.Default.Info)
}

@Composable
fun MayaNavGraph(
    app: MayaApplication,
    initialRoute: String = Screen.Dashboard.route,
    targetRoute: kotlinx.coroutines.flow.StateFlow<String?>? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Reactively navigate if a target route is requested externally (e.g. notification click)
    targetRoute?.let { flow ->
        val routeToOpen by flow.collectAsState()
        LaunchedEffect(routeToOpen) {
            routeToOpen?.let { dest ->
                navController.navigate(dest) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.LiveCall,
        Screen.ThreatDetails,
        Screen.History,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NavySurface,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanAccent,
                            selectedTextColor = CyanAccent,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary,
                            indicatorColor = NavySurfaceElevated
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    sessionManager = app.sessionManager,
                    onNavigateToLiveCall = { navController.navigate(Screen.LiveCall.route) },
                    onNavigateToThreatDetails = { navController.navigate(Screen.ThreatDetails.route) },
                    onNavigateToCapabilities = { navController.navigate(Screen.Capabilities.route) }
                )
            }

            composable(Screen.LiveCall.route) {
                LiveCallGuardianScreen(
                    sessionManager = app.sessionManager,
                    onNavigateToThreatDetails = { navController.navigate(Screen.ThreatDetails.route) }
                )
            }

            composable(Screen.ThreatDetails.route) {
                ThreatDetailsScreen(
                    sessionManager = app.sessionManager
                )
            }

            composable(Screen.History.route) {
                CallHistoryScreen(
                    historyRepository = app.historyRepository
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsRepository = app.settingsRepository,
                    modelRegistry = app.modelRegistry
                )
            }

            composable(Screen.Capabilities.route) {
                CapabilityScreen(
                    capabilityDetector = app.capabilityDetector
                )
            }
        }
    }
}
