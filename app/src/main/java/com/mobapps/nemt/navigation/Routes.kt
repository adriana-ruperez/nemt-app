package com.mobapps.nemt.navigation

sealed class Routes(val route: String) {
    object Welcome : Routes("welcome")
    object Login : Routes("login")
    object Register : Routes("register")
    object VerifyEmail : Routes("verify_email")
    object Home : Routes("home")
    object Trips : Routes("trips")
    object Profile : Routes("profile")
    object Booking : Routes("booking")
}
