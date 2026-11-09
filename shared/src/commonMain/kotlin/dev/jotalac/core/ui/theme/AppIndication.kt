package dev.jotalac.core.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The color of text and icons on the surface they are on - set by [dev.jotalac.core.ui.design.Surface]. */
val LocalContentColor = compositionLocalOf { Color.Unspecified }

/** The style of text, unless it has one of its own. */
val LocalTextStyle = compositionLocalOf { TextStyle.Default }

private const val HOVER_ALPHA = 0.06f
private const val PRESS_ALPHA = 0.11f

/**
 * Hover and press shown as a soft fill of the content's color over the component - no ripple. The component clips
 * it to its shape.
 */
internal object AppIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = AppIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = -1
}

private class AppIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val alpha = Animatable(0f)
    private var animation: Job? = null

    override fun onAttach() {
        coroutineScope.launch {
            val hovers = mutableListOf<HoverInteraction.Enter>()
            val presses = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> hovers += interaction
                    is HoverInteraction.Exit -> hovers -= interaction.enter
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                }
                val target = when {
                    presses.isNotEmpty() -> PRESS_ALPHA
                    hovers.isNotEmpty() -> HOVER_ALPHA
                    else -> 0f
                }
                animation?.cancel()
                animation = launch { alpha.animateTo(target, tween(durationMillis = 120)) }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val value = alpha.value
        if (value > 0f) {
            val color = currentValueOf(LocalContentColor).takeIf { it.isSpecified } ?: Color.Black
            drawRect(color.copy(alpha = value))
        }
    }
}
