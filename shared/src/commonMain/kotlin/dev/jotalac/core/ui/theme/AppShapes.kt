package dev.jotalac.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class AppShapes(
    /** Menu items, icon buttons, rows, tooltips. */
    val small: Shape = RoundedCornerShape(4.dp),
    /** Fields, buttons, snackbars. */
    val medium: Shape = RoundedCornerShape(6.dp),
    /** Dialogs, menus, cards. */
    val large: Shape = RoundedCornerShape(8.dp),
    /** A bottom sheet - rounded at its top only. */
    val sheet: Shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
    val pill: Shape = RoundedCornerShape(percent = 50),
)

internal val LocalAppShapes = staticCompositionLocalOf { AppShapes() }
