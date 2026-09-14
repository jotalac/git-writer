package dev.jotalac.core.di

import android.content.Context
import androidx.activity.ComponentActivity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

fun initKoinAndroid(context: Context) {
    startKoin {
        androidContext(context)
        modules(appModules)
    }
}

fun initFileKitAndroid(activity: ComponentActivity) {
    FileKit.init(activity)
}