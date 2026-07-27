import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

fun main() {
    SystemFileSystem.createDirectories(Path("testDir"))
}
