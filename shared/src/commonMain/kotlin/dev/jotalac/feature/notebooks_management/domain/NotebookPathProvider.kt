package dev.jotalac.feature.notebooks_management.domain

interface NotebookPathProvider {
    fun getDefaultNotebookDirectory(): String
}