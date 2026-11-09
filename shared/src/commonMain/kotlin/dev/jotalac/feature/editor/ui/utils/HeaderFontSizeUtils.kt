package dev.jotalac.feature.editor.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import dev.jotalac.core.ui.theme.AppTheme

@Composable
fun getHeaderFontSize(headingLevel: Int): TextStyle = when (headingLevel) {
    1 -> AppTheme.typography.display.copy(
        fontWeight = FontWeight.Bold,
    )

    2 -> AppTheme.typography.display.copy(
        fontWeight = FontWeight.Bold,
    )

    3 -> AppTheme.typography.title.copy(
        fontWeight = FontWeight.Bold,
    )

    4 -> AppTheme.typography.title.copy(
        fontWeight = FontWeight.SemiBold,
    )

    5 -> AppTheme.typography.title.copy(
        fontWeight = FontWeight.Medium,
    )

    6 -> AppTheme.typography.heading.copy()
    else -> AppTheme.typography.bodyLarge.copy()
}

fun getHeaderLevel(text: String): Int = when {
    text.startsWith("###### ") -> 6
    text.startsWith("##### ") -> 5
    text.startsWith("#### ") -> 4
    text.startsWith("### ") -> 3
    text.startsWith("## ") -> 2
    text.startsWith("# ") -> 1
    else -> 0
}