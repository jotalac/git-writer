package dev.jotalac

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.jotalac.core.di.initKoin

fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "git-writer",
            undecorated = false,
        ) {
            // for custom title bar
//            Column(modifier = Modifier.fillMaxSize()) {
//                WindowDraggableArea {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(MaterialTheme.colors.background)
//                            .height(50.dp)
//                    ) {
//
//                    }
//                }
//                App()
//            }
                App()
        }
    }
}