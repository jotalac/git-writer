package dev.jotalac.feature.notebooks_management.domain

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosNotebookPathProvider : NotebookPathProvider {
    @OptIn(ExperimentalForeignApi::class)
    override fun getDefaultNotebookDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentDirectory = paths.first() as String

        return  "$documentDirectory/git-writer-notebooks"

    }
}