package dev.jotalac.core.utils

fun String.toSafeFileName(): String {
    val invalidCharacters = Regex("[\\\\/:*?\"<>|]")

    return this
        .replace(invalidCharacters, "_")
        .trim()
        .trimEnd('.')
}