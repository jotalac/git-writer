package dev.jotalac.core.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.core.ui.theme.LocalContentColor
import dev.jotalac.core.ui.theme.LocalTextStyle
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.arrow_down
import org.jetbrains.compose.resources.painterResource

/**
 * An outlined field showing the [selected] one of [options] - clicked it opens them in a menu as wide as the field.
 * Each is shown by [optionLabel].
 */
@Composable
fun <T> Select(
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            CompositionLocalProvider(
                LocalContentColor provides colors.textSecondary,
                LocalTextStyle provides AppTheme.typography.labelSmall,
            ) {
                Box(Modifier.padding(start = 4.dp)) { label() }
            }
        }

        Box {
            Surface(
                onClick = { expanded = true },
                enabled = enabled,
                shape = AppTheme.shapes.medium,
                color = colors.field,
                border = colors.border,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { fieldWidth = it.width },
            ) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = AppTheme.dimensions.controlHeight)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = optionLabel(selected),
                        style = AppTheme.typography.body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        painter = painterResource(Res.drawable.arrow_down),
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp).rotate(chevronRotation),
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(with(LocalDensity.current) { fieldWidth.toDp() }),
            ) {
                for (option in options) {
                    MenuItem(
                        text = { Text(optionLabel(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                        contentColor = if (option == selected) colors.accent else colors.text,
                    )
                }
            }
        }
    }
}
