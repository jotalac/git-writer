package dev.jotalac.feature.editor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import dev.jotalac.core.ui.design.FloatingButton
import dev.jotalac.core.ui.design.Icon
import dev.jotalac.core.ui.design.Tooltip
import dev.jotalac.core.ui.design.TooltipPlacement
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
    // unreachable stays enabled - the check can be wrong, and a sync is the way to retry (offline is only in mobile - that is reliable)
    val isEnabled = gitSyncStatus !is GitSyncStatus.Syncing &&
        gitSyncStatus !is GitSyncStatus.GitSyncNotConfigured &&
        gitSyncStatus !is GitSyncStatus.Offline

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
        is GitSyncStatus.UpToDate -> AppTheme.colors.accentSoft
        is GitSyncStatus.Syncing -> AppTheme.colors.accentSoft
        is GitSyncStatus.GitSyncFailed -> AppTheme.colors.dangerSoft
        is GitSyncStatus.Conflict -> AppTheme.colors.danger
        is GitSyncStatus.GitSyncNotConfigured -> AppTheme.colors.surfaceSunken
        is GitSyncStatus.Offline -> AppTheme.colors.surfaceSunken
        is GitSyncStatus.RemoteUnreachable -> AppTheme.colors.highlightSoft
    }

    val targetContentColor = when (gitSyncStatus) {
        is GitSyncStatus.UpToDate -> AppTheme.colors.onAccentSoft
        is GitSyncStatus.Syncing -> AppTheme.colors.onAccentSoft
        is GitSyncStatus.GitSyncFailed -> AppTheme.colors.onDangerSoft
        is GitSyncStatus.Conflict -> AppTheme.colors.onDanger
        is GitSyncStatus.GitSyncNotConfigured -> AppTheme.colors.textMuted
        is GitSyncStatus.Offline -> AppTheme.colors.textMuted
        is GitSyncStatus.RemoteUnreachable -> AppTheme.colors.highlight
    }

    val targetShadowColor = when (gitSyncStatus) {
        is GitSyncStatus.UpToDate -> AppTheme.colors.accentSoft.copy(alpha = 0.25f)
        is GitSyncStatus.Syncing -> AppTheme.colors.onAccentSoft.copy(alpha = 0.1f)
        is GitSyncStatus.GitSyncFailed -> AppTheme.colors.danger.copy(alpha = 0.05f)
        is GitSyncStatus.Conflict -> AppTheme.colors.danger.copy(alpha = 0.1f)
        is GitSyncStatus.GitSyncNotConfigured -> Color.Transparent
        is GitSyncStatus.Offline -> Color.Transparent
        is GitSyncStatus.RemoteUnreachable -> AppTheme.colors.highlight.copy(alpha = 0.1f)
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

    val description = stringResource(getContentDescription(gitSyncStatus))

    // the status in words - after hovering a while on desktop, on long press on mobile
    Tooltip(
        text = description,
        placement = TooltipPlacement.Left,
    ) {
        FloatingButton(
            onClick = { if (isEnabled) onClick() },
            modifier = modifier.dropShadow(
                shape = AppTheme.shapes.large,
                shadow = Shadow(
                    radius = 10.dp,
                    color = animatedShadowColor,
                    spread = 5.dp
                )
            ),
            containerColor = animatedContainerColor,
            contentColor = animatedContentColor,
            shape = AppTheme.shapes.large,
        ) {
            Icon(
                painter = painterResource(getIcon(gitSyncStatus)),
                contentDescription = description,
                modifier = if (isSyncing) Modifier.rotate(rotationAngle) else Modifier
            )
        }
    }
}

private fun getIcon(gitSyncStatus: GitSyncStatus): DrawableResource = when (gitSyncStatus) {
    is GitSyncStatus.UpToDate -> Res.drawable.sync_success
    is GitSyncStatus.Syncing -> Res.drawable.sync_progress
    is GitSyncStatus.GitSyncFailed -> Res.drawable.sync_error
    is GitSyncStatus.Conflict -> Res.drawable.git_merge_conflict
    is GitSyncStatus.GitSyncNotConfigured -> Res.drawable.sync_lock
    is GitSyncStatus.Offline -> Res.drawable.wifi_off
    is GitSyncStatus.RemoteUnreachable -> Res.drawable.cloud_off
}

private fun getContentDescription(gitSyncStatus: GitSyncStatus): StringResource = when (gitSyncStatus) {
    is GitSyncStatus.UpToDate -> Res.string.sync_status_up_to_date
    is GitSyncStatus.Syncing -> Res.string.sync_status_syncing
    is GitSyncStatus.GitSyncFailed -> Res.string.sync_status_failed
    is GitSyncStatus.Conflict -> Res.string.sync_status_conflict
    is GitSyncStatus.GitSyncNotConfigured -> Res.string.sync_status_not_configured
    is GitSyncStatus.Offline -> Res.string.sync_status_offline
    is GitSyncStatus.RemoteUnreachable -> Res.string.sync_status_remote_unreachable
}

@Preview
@Composable
private fun SyncFloatingButtonPreview() {
    AppTheme {
        SyncFloatingButton(gitSyncStatus = GitSyncStatus.UpToDate, onClick = {})
    }
}