package com.mobapps.nemt.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mobapps.nemt.ui.screens.BookingScreen
import com.mobapps.nemt.ui.screens.HomeScreen
import com.mobapps.nemt.ui.screens.ProfileScreen
import com.mobapps.nemt.ui.screens.TripsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Home.route
    ) {
        composable(Routes.Home.route) {
            HomeScreen(
                onGoToTrips = {
                    navController.navigate(Routes.Trips.route)
                },
                onGoToProfile = {
                    navController.navigate(Routes.Profile.route)
                },
                onGoToBooking = {
                    navController.navigate(Routes.Booking.route)
                }
            )
        }

        composable(Routes.Trips.route) {
            TripsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Booking.route) {
            BookingScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}