package dev.jotalac.window

/** Runtime OS detection for the few places where window decoration genuinely differs. */
internal val isMacOs: Boolean =
    System.getProperty("os.name").lowercase().startsWith("mac")

internal val isWindows: Boolean =
    System.getProperty("os.name").lowercase().startsWith("win")
