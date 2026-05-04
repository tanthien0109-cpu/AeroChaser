package com.aerochaser.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aerochaser.presentation.cloud.CloudImportScreen
import com.aerochaser.presentation.detail.PhotoDetailScreen
import com.aerochaser.presentation.detail.PhotoDetailViewModel
import com.aerochaser.presentation.importing.ImportScreen
import com.aerochaser.presentation.timeline.TimelineScreen
import org.koin.androidx.compose.koinViewModel

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem("Timeline", Icons.Default.PhotoLibrary, Screen.Timeline.route),
        BottomNavItem("Import", Icons.Default.FolderOpen, Screen.Import.route),
        BottomNavItem("Cloud", Icons.Default.CloudUpload, Screen.Cloud.route)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Hide bottom bar on detail screen
            if (currentRoute != Screen.Detail.route) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val currentDestination = navBackStackEntry?.destination
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
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
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timeline.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Timeline.route) {
                TimelineScreen(
                    viewModel = koinViewModel(),
                    onPhotoClick = { photoId ->
                        navController.navigate(Screen.Detail.createRoute(photoId))
                    }
                )
            }
            composable(Screen.Import.route) {
                ImportScreen()
            }
            composable(Screen.Cloud.route) {
                CloudImportScreen()
            }
            composable(Screen.Detail.route) { backStackEntry ->
                val photoId = backStackEntry.arguments?.getString("photoId")
                if (photoId != null) {
                    val viewModel: PhotoDetailViewModel = koinViewModel()
                    LaunchedEffect(photoId) {
                        viewModel.loadPhoto(photoId)
                    }
                    val photo by viewModel.photo.collectAsState()
                    val gearProfile by viewModel.gearProfile.collectAsState()
                    val isGearLoading by viewModel.isGearLoading.collectAsState()
                    
                    if (photo != null) {
                        PhotoDetailScreen(
                            photo = photo!!,
                            gearProfile = gearProfile,
                            isGearLoading = isGearLoading,
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
