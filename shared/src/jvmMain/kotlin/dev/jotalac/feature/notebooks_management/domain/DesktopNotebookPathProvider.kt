package dev.jotalac.feature.notebooks_management.domain

import java.io.File

class DesktopNotebookPathProvider : NotebookPathProvider {
    override fun getDefaultNotebookDirectory(): String {
        return System.getProperty("user.home") + "/Documents/git-writer-notebooks"
    }
}