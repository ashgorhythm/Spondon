package com.spondon.app.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun SpondonBottomNav(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        BottomNavItem("Home", "home", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("Community", "community_list", Icons.Filled.Groups, Icons.Outlined.Groups),
        BottomNavItem("Request", "create_request", Icons.Filled.Bloodtype, Icons.Outlined.Bloodtype),
        BottomNavItem("Donors", "find_donor", Icons.Filled.Search, Icons.Outlined.Search),
        BottomNavItem("Profile", "profile", Icons.Filled.Person, Icons.Outlined.Person),
    )
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, item.label) },
                label = { Text(item.label) },
            )
        }
    }
}