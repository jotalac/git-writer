package dev.jotalac.feature.notebooks_management.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.destination_directory_label
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun DirectoryPickerRow(
    directory: String?,
    onBrowseClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.destination_directory_label),
            style = MaterialTheme.typography.labelMedium
        )

        Text(
            text = if (!directory.isNullOrBlank()) directory else "No directory",
            style = MaterialTheme.typography.bodySmall,
            color = if (directory.isNullOrBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
        )
    }
}