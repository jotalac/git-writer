package dev.jotalac.input

import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.KeyEvent


/** Gives the letter keys pressed with Ctrl, Alt or Meta the key code of their place on a US keyboard - X11 only. */
fun fixX11ShortcutKeys() {
    if (Toolkit.getDefaultToolkit().javaClass.name != "sun.awt.X11.XToolkit") return

    // runs before the event reaches Compose, which reads the key code later
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { event ->
        if (event.isShortcutKey() && !event.keyCode.isLatinLetter()) {
            event.rawCode()?.let(US_LETTER_KEYS::get)?.let(event::setKeyCode)
        }
        false
    }
}

private fun KeyEvent.isShortcutKey(): Boolean =
    // the typed events carry the character, they never have a key code
    id != KeyEvent.KEY_TYPED && (isControlDown || isAltDown || isMetaDown)

private fun Int.isLatinLetter(): Boolean = this in KeyEvent.VK_A..KeyEvent.VK_Z

private val RAW_CODE = Regex("""rawCode=(\d+)""")

// the X keycode of the key, the same in every layout - only in the event's description, the field is private
private fun KeyEvent.rawCode(): Int? = RAW_CODE.find(paramString())?.groupValues?.get(1)?.toIntOrNull()

// the X keycodes (evdev) of the letter keys of a US keyboard, row by row
private val US_LETTER_KEYS: Map<Int, Int> = buildMap {
    fun row(firstCode: Int, letters: String) =
        letters.forEachIndexed { i, letter -> put(firstCode + i, KeyEvent.getExtendedKeyCodeForChar(letter.code)) }

    row(24, "QWERTYUIOP")
    row(38, "ASDFGHJKL")
    row(52, "ZXCVBNM")
}
