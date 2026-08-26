package dev.jotalac.feature.settings.platform

import androidx.compose.runtime.Composable

@Composable
actual fun UseDynamicColorSetting(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    // no dynamic color on JVM
}