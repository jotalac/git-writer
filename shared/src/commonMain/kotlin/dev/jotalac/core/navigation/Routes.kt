package dev.jotalac.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Editor : Route

    @Serializable
    data object MainApp : Route
}