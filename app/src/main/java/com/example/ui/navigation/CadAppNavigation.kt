package com.example.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.cad.engine.CadEngine
import com.example.cad.repository.DrawingRepository
import com.example.ui.screens.editor.CadEditorScreen
import com.example.ui.screens.editor.CadEditorViewModel
import com.example.ui.screens.files.FileBrowserScreen
import com.example.ui.screens.files.FileBrowserViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.recent.RecentDrawingsScreen
import com.example.ui.screens.recent.RecentDrawingsViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadSurfaceDark

@Composable
fun CadAppNavigation(
    navController: NavHostController,
    repository: DrawingRepository,
    cadEngine: CadEngine,
    initialDrawingId: String? = null,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check if we are in the CAD Editor screen
    val isEditorScreen = currentRoute?.startsWith("editor") == true

    // Navigate to opened drawing from external intent if provided
    androidx.compose.runtime.LaunchedEffect(initialDrawingId) {
        if (!initialDrawingId.isNullOrEmpty()) {
            navController.navigate(CadNavDestination.Editor.createRoute(initialDrawingId))
        }
    }

    // Factory instances for ViewModels
    val homeViewModel = remember { HomeViewModel(repository, cadEngine) }
    val recentViewModel = remember { RecentDrawingsViewModel(repository) }
    val fileBrowserViewModel = remember { FileBrowserViewModel(repository, cadEngine) }
    val editorViewModel = remember { CadEditorViewModel(repository, cadEngine) }
    val settingsViewModel = remember { SettingsViewModel(cadEngine) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet && !isEditorScreen) {
            // Adaptive Tablet Layout: Side Navigation Rail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("tablet_nav_rail"),
                    containerColor = CadSurfaceDark,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    header = {
                        Spacer(modifier = Modifier.statusBarsPadding())
                        Spacer(modifier = Modifier.height(12.dp))
                        // CAD Mini technical logo mark
                        Canvas(modifier = Modifier.size(28.dp)) {
                            drawCircle(CadCyan, radius = 6f, center = Offset(size.width / 2, size.height / 2))
                            drawLine(Color(0xFF2979FF), Offset(size.width / 2, 4f), Offset(size.width / 2, size.height - 4f), 2f)
                            drawLine(Color(0xFF00E5FF), Offset(4f, size.height / 2), Offset(size.width - 4f, size.height / 2), 2f)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                ) {
                    CadNavDestination.topLevelDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.title
                                )
                            },
                            label = {
                                Text(
                                    text = dest.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    )
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CadCyan,
                                selectedTextColor = CadCyan,
                                indicatorColor = CadCyan.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_rail_item_${dest.route}")
                        )
                    }
                }

                // Vertical border separator
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(CadBorderDark)
                )

                // Main Content
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    CadNavHost(
                        navController = navController,
                        homeViewModel = homeViewModel,
                        recentViewModel = recentViewModel,
                        fileBrowserViewModel = fileBrowserViewModel,
                        editorViewModel = editorViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        } else {
            // Phone / Compact Layout: Scaffold with Bottom Navigation Bar
            Scaffold(
                bottomBar = {
                    if (!isEditorScreen) {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("cad_bottom_nav_bar"),
                            containerColor = CadSurfaceDark,
                            tonalElevation = 8.dp
                        ) {
                            CadNavDestination.topLevelDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CadCyan,
                                        selectedTextColor = CadCyan,
                                        indicatorColor = CadCyan.copy(alpha = 0.15f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                CadNavHost(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    recentViewModel = recentViewModel,
                    fileBrowserViewModel = fileBrowserViewModel,
                    editorViewModel = editorViewModel,
                    settingsViewModel = settingsViewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun CadNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    recentViewModel: RecentDrawingsViewModel,
    fileBrowserViewModel: FileBrowserViewModel,
    editorViewModel: CadEditorViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = CadNavDestination.Home.route,
        modifier = modifier
    ) {
        composable(CadNavDestination.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onOpenDrawing = { drawingId ->
                    navController.navigate(CadNavDestination.Editor.createRoute(drawingId))
                },
                onNavigateToRecent = {
                    navController.navigate(CadNavDestination.Recent.route)
                },
                onNavigateToFiles = {
                    navController.navigate(CadNavDestination.Files.route)
                }
            )
        }

        composable(CadNavDestination.Recent.route) {
            RecentDrawingsScreen(
                viewModel = recentViewModel,
                onOpenDrawing = { drawingId ->
                    navController.navigate(CadNavDestination.Editor.createRoute(drawingId))
                }
            )
        }

        composable(CadNavDestination.Files.route) {
            FileBrowserScreen(
                viewModel = fileBrowserViewModel,
                onOpenDrawing = { drawingId ->
                    navController.navigate(CadNavDestination.Editor.createRoute(drawingId))
                }
            )
        }

        composable(
            route = CadNavDestination.Editor.route,
            arguments = listOf(
                navArgument("drawingId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val drawingId = backStackEntry.arguments?.getString("drawingId")
            CadEditorScreen(
                viewModel = editorViewModel,
                drawingId = drawingId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(CadNavDestination.Settings.route) {
            SettingsScreen(viewModel = settingsViewModel)
        }
    }
}
