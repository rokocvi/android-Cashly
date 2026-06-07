package com.example.projektmobpravi.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.projektmobpravi.ui.navigation.Screen
import com.example.projektmobpravi.ui.theme.SurfaceCard
import com.example.projektmobpravi.ui.theme.TextMuted

data class BottomNavItem(
    val matchRoute: String,
    val navigateRoute: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        matchRoute     = Screen.Home.route,
        navigateRoute  = Screen.Home.route,
        selectedIcon   = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        label          = "Početna"
    ),
    BottomNavItem(
        matchRoute     = Screen.Stats.route,
        navigateRoute  = Screen.Stats.route,
        selectedIcon   = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        label          = "Statistike"
    ),
    BottomNavItem(
        matchRoute     = Screen.AddTransaction.route,
        navigateRoute  = Screen.AddTransaction.addRoute,
        selectedIcon   = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline,
        label          = "Dodaj"
    ),
    BottomNavItem(
        matchRoute     = Screen.Budget.route,
        navigateRoute  = Screen.Budget.route,
        selectedIcon   = Icons.Filled.Wallet,
        unselectedIcon = Icons.Outlined.Wallet,
        label          = "Budgeti"
    )
)

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor  = SurfaceCard,
        tonalElevation  = 0.dp,
        modifier        = Modifier.shadow(
            elevation  = 24.dp,
            spotColor  = Color.Black.copy(alpha = 0.07f),
            ambientColor = Color.Black.copy(alpha = 0.04f)
        )
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.matchRoute
            NavigationBarItem(
                selected = selected,
                onClick  = {
                    if (currentRoute != item.matchRoute) {
                        navController.navigate(item.navigateRoute) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector     = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier        = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text       = item.label,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
