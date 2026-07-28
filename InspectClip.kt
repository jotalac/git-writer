import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard

fun main() {
    val methods = ClipEntry::class.java.methods.joinToString("\n") { it.name }
    println("Methods: $methods")
}
