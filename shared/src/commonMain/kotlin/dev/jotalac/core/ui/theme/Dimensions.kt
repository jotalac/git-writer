package dev.jotalac.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class AppDimensions(
    val iconSmall: Dp,
    val iconMedium: Dp,
    val iconLarge: Dp,
    val buttonCompact: Dp,
    val buttonStandard: Dp,
    val navDrawerWidth: Dp,
    val listItemTextSize: TextUnit,
    val badgeTextSize: TextUnit,
    val listItemPaddingVertical: Dp,
    val listItemIndentPerLevel: Dp
)

// Compact dimensions optimized for mouse pointer precision on Desktop
val DesktopDimensions = AppDimensions(
    iconSmall = 14.dp,
    iconMedium = 18.dp,
    iconLarge = 24.dp,
    buttonCompact = 32.dp,
    buttonStandard = 40.dp,
    navDrawerWidth = 260.dp,
    listItemTextSize = 14.sp,
    badgeTextSize = 11.sp,
    listItemPaddingVertical = 6.dp,
    listItemIndentPerLevel = 12.dp
)

// Touch-friendly dimensions optimized for fingers and legibility on Mobile (Android/iOS)
val MobileDimensions = AppDimensions(
    iconSmall = 20.dp,
    iconMedium = 24.dp,
    iconLarge = 28.dp,
    buttonCompact = 44.dp,
    buttonStandard = 48.dp,
    navDrawerWidth = 360.dp,
    listItemTextSize = 17.sp,
    badgeTextSize = 13.sp,
    listItemPaddingVertical = 12.dp,
    listItemIndentPerLevel = 16.dp
)

val LocalAppDimensions = staticCompositionLocalOf { DesktopDimensions }

val MaterialTheme.dimensions: AppDimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalAppDimensions.current
