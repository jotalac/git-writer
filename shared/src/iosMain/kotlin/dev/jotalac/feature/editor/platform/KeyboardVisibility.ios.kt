package dev.jotalac.feature.editor.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
actual fun isKeyboardVisible(): Boolean = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp