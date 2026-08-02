package dev.jotalac.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.map.Mapper
import dev.jotalac.feature.notebooks_management.domain.NotebookRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.coil.PlatformFileFetcher
import org.koin.compose.koinInject

@Composable
fun ConfigureAppImageLoader() {
    val notebookRepository = koinInject<NotebookRepository>()
    val activeNotebookState = notebookRepository.activeNotebookState.collectAsState(initial = null)

    setSingletonImageLoaderFactory { context ->
        getAsyncImageLoader(context) { activeNotebookState.value?.directoryPath }
    }
}

fun getAsyncImageLoader(
    context: PlatformContext,
    getActiveNotebookDirectory: () -> String? = { null }
) = ImageLoader.Builder(context)
    .components {
        add(Mapper<String, PlatformFile> { data, _ ->
            if (data.startsWith("http://") || data.startsWith("https://")) {
                null // Coil's HTTP fetcher handles network URLs
            } else {
                val cleanPath = data
                    .removePrefix("file://")
                    .removePrefix("<")
                    .removeSuffix(">")
                    .trim()

                val isAbsolute = cleanPath.startsWith("/") ||
                        cleanPath.startsWith("\\") ||
                        cleanPath.matches(Regex("^[a-zA-Z]:[/\\\\].*"))

                val finalPath = if (isAbsolute) {
                    cleanPath
                } else {
                    val notebookRoot = getActiveNotebookDirectory()
                    if (notebookRoot != null) {
                        val cleanRoot = notebookRoot.trimEnd('/', '\\')
                        val relativePath = cleanPath.removePrefix("./").removePrefix(".\\")
                        "$cleanRoot/$relativePath"
                    } else {
                        cleanPath
                    }
                }
                PlatformFile(finalPath)
            }
        })
        add(PlatformFileFetcher.Factory())
    }
    .build()
