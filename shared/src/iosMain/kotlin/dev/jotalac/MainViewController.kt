package dev.jotalac

import androidx.compose.ui.window.ComposeUIViewController
import dev.jotalac.core.di.initKoin
import platform.UIKit.UIViewController

/*
* ios entry point
* initializes koin
 */
fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
