package dev.jotalac.core.di

import io.github.vinceglb.filekit.FileKit

fun initFileKitJvm(appId: String) {
    FileKit.init(appId)
}