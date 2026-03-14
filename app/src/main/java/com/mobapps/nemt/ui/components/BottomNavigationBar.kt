package com.mobapps.nemt.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mobapps.nemt.navigation.Routes

private val BottomBarBackground = Color(0xF0101218)
private val ActiveColor = Color(0xFF2F8FFF)
private val InactiveColor = Color(0xFF7A7F8C)
private val BorderSubtle = Color(0xFFE7E8EE)

data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val bottomBarItems = listOf(
    BottomBarItem(
        route = Routes.Home.route,
        label = "Home",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home
    ),
    BottomBarItem(
        route = Routes.Trips.route,
        label = "Rides",
        icon = Icons.Outlined.CalendarMonth,
        selectedIcon = Icons.Filled.CalendarMonth
    ),
    BottomBarItem(
        route = Routes.Profile.route,
        label = "Profile",
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person
    )
)

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .border(
                width = 0.4.dp,
                color = BorderSubtle
            ),
        containerColor = BottomBarBackground,
        tonalElevation = 0.dp
    ) {
        bottomBarItems.forEach { item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(text = item.label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ActiveColor,
                    selectedTextColor = ActiveColor,
                    unselectedIconColor = InactiveColor,
                    unselectedTextColor = InactiveColor,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}