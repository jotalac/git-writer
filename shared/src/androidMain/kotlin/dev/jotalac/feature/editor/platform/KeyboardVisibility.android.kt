package dev.jotalac.feature.editor.platform

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalLayoutApi::class)
@Composable
actual fun isKeyboardVisible(): Boolean {
    println(
        "imeBottom=${
            WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        }  isImeVisible=${WindowInsets.isImeVisible}"
    )
    return WindowInsets.isImeVisible
}