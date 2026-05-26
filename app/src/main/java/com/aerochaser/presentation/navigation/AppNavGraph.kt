package com.aerochaser.presentation.navigation

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    var activePhotoId by rememberSaveable { mutableStateOf<String?>(null) }
    var splitRatio by rememberSaveable { mutableStateOf(0.4f) }

    val bottomNavItems = listOf(
        BottomNavItem("Timeline", Icons.Default.PhotoLibrary, Screen.Timeline.route),
        BottomNavItem("Import", Icons.Default.FolderOpen, Screen.Import.route),
        BottomNavItem("Cloud", Icons.Default.CloudUpload, Screen.Cloud.route)
    )

    // Handle seamless folding/unfolding state transitions
    LaunchedEffect(isWideScreen) {
        if (isWideScreen) {
            val currentEntry = navController.currentBackStackEntry
            if (currentEntry?.destination?.route == Screen.Detail.route) {
                val photoId = currentEntry.arguments?.getString("photoId")
                if (photoId != null) {
                    activePhotoId = photoId
                    navController.popBackStack()
                }
            }
        } else {
            activePhotoId?.let { photoId ->
                navController.navigate(Screen.Detail.createRoute(photoId))
                activePhotoId = null
            }
        }
    }

    if (isWideScreen) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Master Column (Timeline / Import / Cloud spanning left column)
            Box(
                modifier = Modifier
                    .weight(splitRatio)
                    .fillMaxHeight()
            ) {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
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
                                    activePhotoId = photoId
                                }
                            )
                        }
                        composable(Screen.Import.route) {
                            ImportScreen()
                        }
                        composable(Screen.Cloud.route) {
                            CloudImportScreen()
                        }
                    }
                }
            }

            // Interactive Draggable Divider (Google & Samsung Split Haptic Guideline)
            var isDragging by remember { mutableStateOf(false) }
            val density = androidx.compose.ui.platform.LocalDensity.current
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp) // wider target for effortless touchscreen drag
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaRatio = dragAmount.x / (configuration.screenWidthDp * density.density)
                                splitRatio = (splitRatio + deltaRatio).coerceIn(0.25f, 0.75f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Crease divider line
                Spacer(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
                // Visual drag handle selector bar
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 48.dp)
                        .background(
                            color = if (isDragging) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }

            // Detail Column (Right Pane)
            Box(
                modifier = Modifier
                    .weight(1f - splitRatio)
                    .fillMaxHeight()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val currentId = activePhotoId
                if (currentId != null) {
                    val detailViewModel: PhotoDetailViewModel = koinViewModel()

                    LaunchedEffect(currentId) {
                        detailViewModel.loadPhotos(currentId)
                    }

                    val allPhotos by detailViewModel.allPhotos.collectAsState()
                    val initialPage by detailViewModel.initialPage.collectAsState()
                    val currentPhoto by detailViewModel.currentPhoto.collectAsState()
                    val gearProfile by detailViewModel.gearProfile.collectAsState()
                    val isGearLoading by detailViewModel.isGearLoading.collectAsState()
                    val locationName by detailViewModel.locationName.collectAsState()
                    val aiSummaryState by detailViewModel.aiSummaryState.collectAsState()

                    PhotoDetailScreen(
                        photos = allPhotos,
                        initialPage = initialPage,
                        currentPhoto = currentPhoto,
                        gearProfile = gearProfile,
                        isGearLoading = isGearLoading,
                        locationName = locationName,
                        aiSummaryState = aiSummaryState,
                        onPageSettled = { photo -> detailViewModel.onPageSettled(photo) },
                        onGenerateAiSummary = { detailViewModel.generateAiSummary() },
                        onBack = { activePhotoId = null }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Select a photo from the timeline to view details",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Standard / Folded single pane view
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

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
                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(navArgument("photoId") { type = NavType.StringType }),
                    enterTransition = {
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    }
                ) { backStackEntry ->
                    val photoId = backStackEntry.arguments?.getString("photoId")
                    if (photoId != null) {
                        val viewModel: PhotoDetailViewModel = koinViewModel()

                        LaunchedEffect(photoId) {
                            viewModel.loadPhotos(photoId)
                        }

                        val allPhotos by viewModel.allPhotos.collectAsState()
                        val initialPage by viewModel.initialPage.collectAsState()
                        val currentPhoto by viewModel.currentPhoto.collectAsState()
                        val gearProfile by viewModel.gearProfile.collectAsState()
                        val isGearLoading by viewModel.isGearLoading.collectAsState()
                        val locationName by viewModel.locationName.collectAsState()
                        val aiSummaryState by viewModel.aiSummaryState.collectAsState()

                        PhotoDetailScreen(
                            photos = allPhotos,
                            initialPage = initialPage,
                            currentPhoto = currentPhoto,
                            gearProfile = gearProfile,
                            isGearLoading = isGearLoading,
                            locationName = locationName,
                            aiSummaryState = aiSummaryState,
                            onPageSettled = { photo -> viewModel.onPageSettled(photo) },
                            onGenerateAiSummary = { viewModel.generateAiSummary() },
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
