package com.example.finanzaspersonales.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class NavigationDestination(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val badgeCount: Int? = null
)

val financeDestinations = listOf(
    NavigationDestination("dashboard", Icons.Default.Dashboard, "Dashboard"),
    NavigationDestination("categories", Icons.Default.Category, "Categories"),
    NavigationDestination("transactions", Icons.Default.AccountBalance, "Transactions"),
    NavigationDestination("providers", Icons.Default.Storefront, "Providers"),
    NavigationDestination("settings", Icons.Default.Settings, "Settings")
)

@Composable
fun AdaptiveNavigationScaffold(
    windowSizeClass: WindowSizeClass,
    currentDestination: String,
    onDestinationClick: (String) -> Unit,
    onAddClick: () -> Unit,
    content: @Composable () -> Unit
) {
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            // Bottom Navigation for phones
            CompactNavigationScaffold(
                currentDestination = currentDestination,
                onDestinationClick = onDestinationClick,
                onAddClick = onAddClick,
                content = content
            )
        }
        WindowWidthSizeClass.Medium -> {
            // Navigation Rail for tablets in portrait or small landscape
            MediumNavigationScaffold(
                currentDestination = currentDestination,
                onDestinationClick = onDestinationClick,
                onAddClick = onAddClick,
                content = content
            )
        }
        WindowWidthSizeClass.Expanded -> {
            // Navigation Drawer for large tablets and desktops
            ExpandedNavigationScaffold(
                currentDestination = currentDestination,
                onDestinationClick = onDestinationClick,
                onAddClick = onAddClick,
                content = content
            )
        }
        else -> {
            // Fallback to compact
            CompactNavigationScaffold(
                currentDestination = currentDestination,
                onDestinationClick = onDestinationClick,
                onAddClick = onAddClick,
                content = content
            )
        }
    }
}

@Composable
private fun CompactNavigationScaffold(
    currentDestination: String,
    onDestinationClick: (String) -> Unit,
    onAddClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                financeDestinations.take(4).forEach { destination -> // Limit to 4 for bottom nav
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                destination.icon, 
                                contentDescription = destination.label
                            ) 
                        },
                        label = { Text(destination.label) },
                        selected = currentDestination == destination.route,
                        onClick = { onDestinationClick(destination.route) }
                    )
                }
                // Add button as the 5th item
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Transaction") },
                    label = { Text("Add") },
                    selected = false,
                    onClick = onAddClick
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            content()
        }
    }
}

@Composable
private fun MediumNavigationScaffold(
    currentDestination: String,
    onDestinationClick: (String) -> Unit,
    onAddClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold { paddingValues ->
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                financeDestinations.forEach { destination ->
                    NavigationRailItem(
                        icon = { 
                            Icon(
                                destination.icon, 
                                contentDescription = destination.label
                            ) 
                        },
                        label = { Text(destination.label) },
                        selected = currentDestination == destination.route,
                        onClick = { onDestinationClick(destination.route) }
                    )
                }
                // Add button
                NavigationRailItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Transaction") },
                    label = { Text("Add") },
                    selected = false,
                    onClick = onAddClick
                )
            }
            
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ExpandedNavigationScaffold(
    currentDestination: String,
    onDestinationClick: (String) -> Unit,
    onAddClick: () -> Unit,
    content: @Composable () -> Unit
) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Finanzas Personales",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    financeDestinations.forEach { destination ->
                        NavigationDrawerItem(
                            icon = { 
                                Icon(
                                    destination.icon, 
                                    contentDescription = destination.label
                                ) 
                            },
                            label = { Text(destination.label) },
                            selected = currentDestination == destination.route,
                            onClick = { onDestinationClick(destination.route) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    // Add button
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add Transaction") },
                        label = { Text("Add Transaction") },
                        selected = false,
                        onClick = onAddClick,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    ) {
        content()
    }
} 