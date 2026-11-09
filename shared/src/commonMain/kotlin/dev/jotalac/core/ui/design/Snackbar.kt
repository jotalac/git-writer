package dev.jotalac.core.ui.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.AppTheme
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class SnackbarDuration(val time: Duration) {
    Short(4.seconds),
    Long(10.seconds),
}

enum class SnackbarResult { Dismissed, ActionPerformed }

/** A message shown by a [SnackbarHost]. */
@Stable
class SnackbarData internal constructor(
    val message: String,
    val actionLabel: String?,
    val duration: SnackbarDuration,
    private val continuation: CancellableContinuation<SnackbarResult>,
) {
    fun performAction() {
        if (continuation.isActive) continuation.resume(SnackbarResult.ActionPerformed)
    }

    fun dismiss() {
        if (continuation.isActive) continuation.resume(SnackbarResult.Dismissed)
    }
}

/** The messages for a [SnackbarHost] - one at a time, the next one waits for the shown one to go. */
@Stable
class SnackbarHostState {
    private val mutex = Mutex()

    var currentSnackbarData by mutableStateOf<SnackbarData?>(null)
        private set

    /** Shows [message] until it times out or is dismissed - what ended it. */
    suspend fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ): SnackbarResult = mutex.withLock {
        try {
            suspendCancellableCoroutine { continuation ->
                currentSnackbarData = SnackbarData(message, actionLabel, duration, continuation)
            }
        } finally {
            currentSnackbarData = null
        }
    }
}

/** Shows the [hostState]'s messages, sliding in from below - each one by [snackbar]. */
@Composable
fun SnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { Snackbar(it) },
) {
    val current = hostState.currentSnackbarData
    // the last one stays for its exit animation
    var shown by remember { mutableStateOf<SnackbarData?>(null) }
    if (current != null) shown = current
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = current != null

    LaunchedEffect(current) {
        if (current == null) return@LaunchedEffect
        delay(current.duration.time)
        current.dismiss()
    }

    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn(tween(150)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it / 2 },
        ) {
            shown?.let { snackbar(it) }
        }
    }
}

/** A message in a raised, outlined card - with its action, if it has one. */
@Composable
fun Snackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Surface(
        shape = AppTheme.shapes.medium,
        color = colors.surfaceRaised,
        border = colors.border,
        shadowElevation = 8.dp,
        modifier = modifier
            .padding(16.dp)
            .widthIn(max = 560.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = data.message,
                style = AppTheme.typography.body,
                modifier = Modifier.weight(1f, fill = false).padding(vertical = 8.dp),
            )
            data.actionLabel?.let { action ->
                TextButton(onClick = data::performAction) {
                    Text(text = action, color = colors.accent)
                }
            }
        }
    }
}
