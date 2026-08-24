package dev.jotalac.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Settings : Route

    @Serializable
    data object MainApp : Route
}