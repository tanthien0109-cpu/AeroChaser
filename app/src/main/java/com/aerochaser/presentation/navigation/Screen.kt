package com.aerochaser.presentation.navigation

/**
 * Defines all navigation routes in the app.
 * Using sealed class for type safety and exhaustive matching.
 */
sealed class Screen(val route: String) {
    object Timeline : Screen("timeline")
    object Import : Screen("import")
    object Cloud : Screen("cloud")
    object Detail : Screen("detail/{photoId}") {
        fun createRoute(photoId: String): String = "detail/$photoId"
    }
}
