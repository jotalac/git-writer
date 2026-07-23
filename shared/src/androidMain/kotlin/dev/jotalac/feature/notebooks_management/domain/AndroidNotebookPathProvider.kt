package dev.jotalac.feature.notebooks_management.domain

import android.content.Context
import java.io.File

class AndroidNotebookPathProvider(private val context: Context) : NotebookPathProvider {
    override fun getDefaultNotebookDirectory(): String {
        val baseDir = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
        return "$baseDir/git-writer-notes"
    }
}