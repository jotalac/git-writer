package dev.jotalac.core.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.ui.theme.LocalContentColor
import dev.jotalac.core.ui.theme.LocalTextStyle

/**
 * An outlined field, its [label] above it. Focused its outline turns to the accent, with a soft ring around it - in
 * the danger color while [isError], which colors the [supportingText] below it too.
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = AppTheme.typography.body,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = AppTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by source.collectIsFocusedAsState()
    val outlineColor by animateColorAsState(
        when {
            isError -> colors.danger
            isFocused -> colors.accent
            else -> colors.border
        }
    )
    val ringAlpha by animateFloatAsState(if (isFocused) 0.25f else 0f)
    val shape = AppTheme.shapes.medium

    Column(
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (label != null) {
            CompositionLocalProvider(
                LocalContentColor provides if (isError) colors.danger else colors.textSecondary,
                LocalTextStyle provides AppTheme.typography.labelSmall,
            ) {
                Box(Modifier.padding(start = 4.dp)) { label() }
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle.merge(color = colors.text),
            cursorBrush = SolidColor(if (isError) colors.danger else colors.accent),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = source,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .focusRing(shape, outlineColor.copy(alpha = ringAlpha))
                        .background(colors.field, shape)
                        .border(1.dp, outlineColor, shape)
                        .defaultMinSize(minHeight = AppTheme.dimensions.controlHeight)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompositionLocalProvider(LocalContentColor provides colors.textSecondary) {
                        leadingIcon?.invoke()
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder != null) {
                            CompositionLocalProvider(
                                LocalContentColor provides colors.textMuted,
                                LocalTextStyle provides textStyle,
                            ) {
                                placeholder()
                            }
                        }
                        innerTextField()
                    }
                    CompositionLocalProvider(LocalContentColor provides colors.textSecondary) {
                        trailingIcon?.invoke()
                    }
                }
            },
        )

        if (supportingText != null) {
            CompositionLocalProvider(
                LocalContentColor provides if (isError) colors.danger else colors.textMuted,
                LocalTextStyle provides AppTheme.typography.bodySmall,
            ) {
                Box(Modifier.padding(start = 4.dp)) { supportingText() }
            }
        }
    }
}

// a ring just outside the field, in [color] - transparent when it is not focused
private fun Modifier.focusRing(shape: Shape, color: Color): Modifier = drawBehind {
    if (color.alpha == 0f) return@drawBehind
    val width = 3.dp.toPx()
    val outline = shape.createOutline(Size(size.width + width, size.height + width), layoutDirection, this)
    translate(-width / 2, -width / 2) {
        drawOutline(outline, color, style = Stroke(width))
    }
}
