package dev.jotalac.window

import androidx.compose.ui.awt.ComposeWindow
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * set themes on windows - ai generated
 */
internal fun ComposeWindow.applyWindowsTitleBarTheme(isDark: Boolean) {
    if (!isWindows) return

    runCatching {
        val windowHandle = Native.getWindowPointer(this) ?: return
        val value = Memory(Int.SIZE_BYTES.toLong())
        value.use { value ->
            value.setInt(0, if (isDark) 1 else 0)
            val applied = DwmApi.instance.DwmSetWindowAttribute(
                windowHandle,
                DWMWA_USE_IMMERSIVE_DARK_MODE,
                value,
                Int.SIZE_BYTES,
            )
            if (applied != 0) {
                DwmApi.instance.DwmSetWindowAttribute(
                    windowHandle,
                    DWMWA_USE_IMMERSIVE_DARK_MODE_LEGACY,
                    value,
                    Int.SIZE_BYTES,
                )
            }
        }
    }.onFailure { error ->
        println("[titlebar] could not theme the Windows title bar: $error")
    }
}

private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
private const val DWMWA_USE_IMMERSIVE_DARK_MODE_LEGACY = 19

private interface DwmApi : Library {
    fun DwmSetWindowAttribute(
        windowHandle: Pointer,
        attribute: Int,
        value: Pointer,
        valueSize: Int,
    ): Int

    companion object {
        val instance: DwmApi = Native.load("dwmapi", DwmApi::class.java)
    }
}
