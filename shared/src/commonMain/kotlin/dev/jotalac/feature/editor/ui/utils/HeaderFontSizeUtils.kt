package dev.jotalac.feature.editor.ui.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
//
//@Composable
//fun getHeaderFontSize(headingLevel: Int): TextStyle = when (headingLevel) {
//    1 -> MaterialTheme.typography.displayMedium.copy(
//        fontWeight = FontWeight.Bold,
//        fontSize = 38.sp,
//        lineHeight = 42.sp,
//    )
//
//    2 -> MaterialTheme.typography.headlineLarge.copy(
//        fontWeight = FontWeight.Bold,
//        fontSize = 28.sp,
//        lineHeight = 36.sp,
//    )
//
//    3 -> MaterialTheme.typography.headlineMedium.copy(
//        fontWeight = FontWeight.Bold,
//        fontSize = 22.sp,
//        lineHeight = 32.sp,
//    )
//
//    4 -> MaterialTheme.typography.headlineSmall.copy(
//        fontWeight = FontWeight.SemiBold,
//        fontSize = 20.sp
//    )
//
//    5 -> MaterialTheme.typography.titleLarge.copy(
//        fontWeight = FontWeight.Medium,
//    )
//
//    6 -> MaterialTheme.typography.titleMedium.copy()
//    else -> MaterialTheme.typography.bodyLarge.copy()
//}

@Composable
fun getHeaderFontSize(headingLevel: Int): TextStyle {
    val body = MaterialTheme.typography.bodyLarge
    val (scale, weight) = when (headingLevel) {
        1 -> 2.2f to FontWeight.Bold
        2 -> 1.6f to FontWeight.Bold
        3 -> 1.35f to FontWeight.Bold
        4 -> 1.2f to FontWeight.SemiBold
        5 -> 1.1f to FontWeight.SemiBold
        6 -> 1.0f to FontWeight.SemiBold
        else -> return body
    }
    return body.copy(
        fontSize = body.fontSize * scale,
        lineHeight = body.fontSize * scale * 1.25f,
        fontWeight = weight,
    )
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