package dev.jotalac.core.utils

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import io.github.vinceglb.filekit.coil.PlatformFileFetcher

@Composable
fun ConfigureAppImageLoader() {
    setSingletonImageLoaderFactory { context ->
        getAsyncImageLoader(context)
    }
}

fun getAsyncImageLoader(context: PlatformContext) =
    ImageLoader.Builder(context)
        .components {
            add(PlatformFileFetcher.Factory())
        }
        .build()
