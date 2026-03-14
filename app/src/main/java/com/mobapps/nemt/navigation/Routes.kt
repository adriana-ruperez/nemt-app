package com.mobapps.nemt.navigation

sealed class Routes(val route: String) {
    object Home : Routes("home")
    object Trips : Routes("trips")
    object Profile : Routes("profile")
    object Booking : Routes("booking")
}