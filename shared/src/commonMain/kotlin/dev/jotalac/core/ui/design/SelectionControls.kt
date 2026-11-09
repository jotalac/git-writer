package dev.jotalac.core.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.ui.theme.LocalContentColor
import dev.jotalac.core.ui.theme.LocalTextStyle

private const val DISABLED_ALPHA = 0.45f

/** An on/off switch - a pill track in the accent when on, its knob sliding over. */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val track by animateColorAsState(if (checked) colors.accent else colors.surfaceStrong)
    val knobOffset by animateDpAsState(if (checked) 18.dp else 2.dp)


    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        onValueChange = onCheckedChange,
                        enabled = enabled,
                        role = Role.Switch,
                        interactionSource = null,
                        indication = null,
                    )
                } else {
                    Modifier
                }
            )
            .size(width = 36.dp, height = 20.dp)
            .background(track, CircleShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(16.dp)
                .shadow(1.dp, CircleShape, ambientColor = colors.shadow, spotColor = colors.shadow)
                .background(if (checked) colors.onAccent else if (colors.isDark) colors.textSecondary else colors.surfaceRaised, CircleShape)
        )
    }
}

/** An outlined square, filled in the accent with a check mark when [checked]. */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val fill by animateColorAsState(if (checked) colors.accent else Color.Transparent)
    val outline by animateColorAsState(if (checked) colors.accent else colors.textMuted)
    val check by animateFloatAsState(if (checked) 1f else 0f)
    val checkColor = colors.onAccent

    Canvas(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        onValueChange = onCheckedChange,
                        enabled = enabled,
                        role = Role.Checkbox,
                        interactionSource = null,
                        indication = null,
                    )
                } else {
                    Modifier
                }
            )
            .padding(2.dp)
            .size(16.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(fill)
            .border(1.5.dp, outline, RoundedCornerShape(3.dp))
    ) {
        if (check > 0f) {
            val path = Path().apply {
                moveTo(size.width * 0.24f, size.height * 0.52f)
                lineTo(size.width * 0.43f, size.height * 0.70f)
                lineTo(size.width * 0.77f, size.height * 0.32f)
            }
            drawPath(
                path,
                color = checkColor.copy(alpha = check),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** A round choice of a group - a dot in the accent when [selected]. */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val ring by animateColorAsState(if (selected) colors.accent else colors.textMuted)
    val dot by animateFloatAsState(if (selected) 1f else 0f)

    Canvas(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(
                if (onClick != null) {
                    Modifier.selectable(
                        selected = selected,
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.RadioButton,
                        interactionSource = null,
                        indication = null,
                    )
                } else {
                    Modifier
                }
            )
            .padding(2.dp)
            .size(16.dp)
    ) {
        val stroke = 1.5.dp.toPx()
        drawCircle(ring, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
        if (dot > 0f) drawCircle(colors.accent, radius = size.minDimension / 4.5f * dot, center = Offset(size.width / 2, size.height / 2))
    }
}

/**
 * One of [options] chosen in a sunken pill - the chosen one raised on it. Each option is shown by [label], in its
 * own [labelFontFamily] when it has one (a font to choose).
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labelFontFamily: @Composable (T) -> FontFamily? = { null },
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(colors.surfaceSunken, AppTheme.shapes.medium)
            .padding(2.dp),
    ) {
        for (option in options) {
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                enabled = enabled,
                shape = AppTheme.shapes.small,
                // outlined on the light track, a step lighter on the dark one
                color = when {
                    !isSelected -> Color.Transparent
                    colors.isDark -> colors.surfaceStrong
                    else -> colors.surfaceRaised
                },
                contentColor = if (isSelected) colors.text else colors.textSecondary,
                border = if (isSelected && !colors.isDark) colors.border else Color.Transparent,
                modifier = Modifier.weight(1f).height(AppTheme.dimensions.controlHeight - 4.dp),
            ) {
                CompositionLocalProvider(LocalTextStyle provides AppTheme.typography.label) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label(option),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = LocalContentColor.current,
                            fontFamily = labelFontFamily(option),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
