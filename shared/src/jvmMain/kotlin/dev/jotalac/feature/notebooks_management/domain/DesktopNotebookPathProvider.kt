package dev.jotalac.feature.notebooks_management.domain

import java.io.File

class DesktopNotebookPathProvider : NotebookPathProvider {
    override fun getDefaultNotebookDirectory(): String {
        val path = System.getProperty("user.home") + "/Documents/git-writer-notebooks"

        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        return path
    }
}