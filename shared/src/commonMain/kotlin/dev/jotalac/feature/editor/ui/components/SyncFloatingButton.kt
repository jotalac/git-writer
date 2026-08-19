package dev.jotalac.feature.editor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jotalac.core.ui.theme.AppTheme
import dev.jotalac.feature.git_sync.domain.GitSyncStatus
import git_writer.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SyncFloatingButton(
    gitSyncStatus: GitSyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSyncing = gitSyncStatus is GitSyncStatus.Syncing
    val isEnabled = gitSyncStatus !is GitSyncStatus.Syncing && gitSyncStatus !is GitSyncStatus.GitSyncNotConfigured

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by if (isSyncing) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sync_progress_rotation"
        )
    } else {
        rememberUpdatedState(0f)
    }

    val targetContainerColor = when (gitSyncStatus) {
        is GitSyncStatus.UpToDate -> MaterialTheme.colorScheme.primaryContainer
        is GitSyncStatus.Syncing -> MaterialTheme.colorScheme.secondaryContainer
        is GitSyncStatus.GitSyncFailed -> MaterialTheme.colorScheme.errorContainer
        is GitSyncStatus.Conflict -> MaterialTheme.colorScheme.error
        is GitSyncStatus.GitSyncNotConfigured -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val targetContentColor = when (gitSyncStatus) {
        is GitSyncStatus.UpToDate -> MaterialTheme.colorScheme.onPrimaryContainer
        is GitSyncStatus.Syncing -> MaterialTheme.colorScheme.onSecondaryContainer
        is GitSyncStatus.GitSyncFailed -> MaterialTheme.colorScheme.onErrorContainer
        is GitSyncStatus.Conflict -> MaterialTheme.colorScheme.onError
        is GitSyncStatus.GitSyncNotConfigured -> MaterialTheme.colorScheme.outline
    }

    val targetShadowColor = when (gitSyncStatus) {
        is GitSyncStatus.UpToDate -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        is GitSyncStatus.Syncing -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
        is GitSyncStatus.GitSyncFailed -> MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
        is GitSyncStatus.Conflict -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        is GitSyncStatus.GitSyncNotConfigured -> Color.Transparent
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 300),
        label = "fabContainerColor"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = tween(durationMillis = 300),
        label = "fabContentColor"
    )

    val animatedShadowColor by animateColorAsState(
        targetValue = targetShadowColor,
        animationSpec = tween(durationMillis = 300),
        label = "fabShadowColor"
    )

    FloatingActionButton(
        onClick = { if (isEnabled) onClick() },
        modifier = modifier.dropShadow(
            shape = CircleShape,
            shadow = Shadow(
                radius = 10.dp,
                color = animatedShadowColor,
                spread = 5.dp
            )
        ),
//        shape = CircleShape,
        containerColor = animatedContainerColor,
        contentColor = animatedContentColor,
    ) {
        Icon(
            painter = painterResource(getIcon(gitSyncStatus)),
            contentDescription = stringResource(getContentDescription(gitSyncStatus)),
            modifier = if (isSyncing) Modifier.rotate(rotationAngle) else Modifier
        )
    }
}

private fun getIcon(gitSyncStatus: GitSyncStatus): DrawableResource = when (gitSyncStatus) {
    is GitSyncStatus.UpToDate -> Res.drawable.sync_success
    is GitSyncStatus.Syncing -> Res.drawable.sync_progress
    is GitSyncStatus.GitSyncFailed -> Res.drawable.sync_error
    is GitSyncStatus.Conflict -> Res.drawable.git_merge_conflict
    is GitSyncStatus.GitSyncNotConfigured -> Res.drawable.sync_lock
}

private fun getContentDescription(gitSyncStatus: GitSyncStatus): StringResource = when (gitSyncStatus) {
    is GitSyncStatus.UpToDate -> Res.string.sync_status_up_to_date
    is GitSyncStatus.Syncing -> Res.string.sync_status_syncing
    is GitSyncStatus.GitSyncFailed -> Res.string.sync_status_failed
    is GitSyncStatus.Conflict -> Res.string.sync_status_conflict
    is GitSyncStatus.GitSyncNotConfigured -> Res.string.sync_status_not_configured
}

@Preview
@Composable
private fun SyncFloatingButtonPreview() {
    AppTheme {
        SyncFloatingButton(gitSyncStatus = GitSyncStatus.UpToDate, onClick = {})
    }
}