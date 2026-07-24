package dev.jotalac.feature.notebooks_management.domain

data class Notebook(
    val id: Long = 0,
    val name: String,
    val directoryPath: String,
    val remoteUrl: String?,
    val remoteUsername: String?,
    val remotePassword: String?,
)
