package com.mobapps.nemt.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mobapps.nemt.ui.components.BottomNavigationBar
import com.mobapps.nemt.ui.screens.BookingScreen
import com.mobapps.nemt.ui.screens.HomeScreen
import com.mobapps.nemt.ui.screens.ProfileScreen
import com.mobapps.nemt.ui.screens.TripsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute == Routes.Home.route ||
            currentRoute == Routes.Trips.route ||
            currentRoute == Routes.Profile.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Routes.Home.route
        ) {
            composable(Routes.Home.route) {
                HomeScreen(
                    onGoToTrips = {
                        navController.navigate(Routes.Trips.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGoToProfile = {
                        navController.navigate(Routes.Profile.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGoToBooking = {
                        navController.navigate(Routes.Booking.route)
                    },
                    contentPadding = innerPadding
                )
            }

            composable(Routes.Trips.route) {
                TripsScreen(
                    onBack = { navController.popBackStack() },
                    contentPadding = innerPadding
                )
            }

            composable(Routes.Profile.route) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    contentPadding = innerPadding
                )
            }

            composable(Routes.Booking.route) {
                BookingScreen(
                    onBack = { navController.popBackStack() },
                    contentPadding = innerPadding
                )
            }
        }
    }
}