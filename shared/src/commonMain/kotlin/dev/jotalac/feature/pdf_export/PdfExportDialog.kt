package dev.jotalac.feature.pdf_export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.jotalac.core.data.UserSettings
import dev.jotalac.core.utils.suspendRunCatching
import git_writer.shared.generated.resources.Res
import git_writer.shared.generated.resources.exporting_pdf
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** A note to export: its name (the suggested file name) and its markdown. */
data class PdfExportRequest(val noteName: String, val content: String)

/**
 * Exports [request] to a PDF: asks where to save it, renders the pages while showing a progress dialog and
 * writes the file. [onFinished] gets the saved file's name, null when the save dialog was cancelled, or why
 * the export failed.
 */
@Composable
fun PdfExportDialog(request: PdfExportRequest, onFinished: (Result<String?>) -> Unit) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    val scope = rememberCoroutineScope()
    // the note is rendered with the font and accent color the user picked
    val userSettings = koinInject<UserSettings>()
    val settings by remember(userSettings) { userSettings.userSettingsStateFlow }.collectAsState(initial = null)
    var target by remember(request) { mutableStateOf<PlatformFile?>(null) }

    LaunchedEffect(request) {
        suspendRunCatching {
            FileKit.openFileSaver(
                suggestedName = request.noteName,
                defaultExtension = "pdf",
                directory = defaultExportDirectory(),
            )
        }
            .onSuccess { file -> if (file == null) currentOnFinished(Result.success(null)) else target = file }
            .onFailure { currentOnFinished(Result.failure(it)) }
    }

    val file = target ?: return

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                Text(stringResource(Res.string.exporting_pdf, request.noteName))
            }

            settings?.let { settings ->
                NotePdfRenderer(request.content, settings.themeAccentColor, settings.font) { pages ->
                    scope.launch {
                        val result = suspendRunCatching {
                            file.write(writeImagePdf(pages.getOrThrow()))
                            file.name
                        }
                        currentOnFinished(result)
                    }
                }
            }
        }
    }
}
