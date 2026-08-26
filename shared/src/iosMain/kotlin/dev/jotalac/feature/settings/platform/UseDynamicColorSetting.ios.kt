package dev.jotalac.feature.settings.platform

@Composable
actual fun UseDynamicColorSetting(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    // no dynamic color on iOS
}