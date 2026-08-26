package dev.jotalac.feature.settings.platform

import androidx.compose.runtime.Composable

@Composable
expect fun UseDynamicColorSetting(isEnabled: Boolean, onToggle: (Boolean) -> Unit)