package dev.jotalac.feature.notebooks_management.domain

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosNotebookPathProvider : NotebookPathProvider {
    override fun getDefaultNotebookDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentDirectory = paths.first() as String

        return "$documentDirectory/git-writer-notebooks"
    }
}