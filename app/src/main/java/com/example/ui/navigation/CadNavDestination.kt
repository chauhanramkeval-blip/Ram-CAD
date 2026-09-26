package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class CadNavDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showInBottomBar: Boolean = true
) {
    data object Home : CadNavDestination(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Recent : CadNavDestination(
        route = "recent",
        title = "Recent",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )

    data object Files : CadNavDestination(
        route = "files",
        title = "Drawings",
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder
    )

    data object Editor : CadNavDestination(
        route = "editor?drawingId={drawingId}",
        title = "CAD Editor",
        selectedIcon = Icons.Filled.Create,
        unselectedIcon = Icons.Outlined.Create,
        showInBottomBar = false
    ) {
        fun createRoute(drawingId: String? = null): String {
            return if (drawingId != null) "editor?drawingId=$drawingId" else "editor"
        }
    }

    data object Settings : CadNavDestination(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    companion object {
        val topLevelDestinations = listOf(Home, Recent, Files, Settings)

        fun fromRoute(route: String?): CadNavDestination {
            return when {
                route == null -> Home
                route.startsWith("home") -> Home
                route.startsWith("recent") -> Recent
                route.startsWith("files") -> Files
                route.startsWith("editor") -> Editor
                route.startsWith("settings") -> Settings
                else -> Home
            }
        }
    }
}
