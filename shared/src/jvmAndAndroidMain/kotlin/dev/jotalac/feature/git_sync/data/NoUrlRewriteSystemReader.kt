package dev.jotalac.feature.git_sync.data

import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader
import java.io.File

class NoUrlRewriteSystemReader(
    private val delegate: SystemReader
) : SystemReader() {
    override fun getHostname(): String = delegate.hostname
    override fun getenv(variable: String?): String? = delegate.getenv(variable)
    override fun getProperty(key: String?): String? = delegate.getProperty(key)

    override fun openUserConfig(
        parent: Config?,
        fs: FS?
    ): FileBasedConfig = stripUrlSection(parent, delegate.openUserConfig(parent, fs), fs)

    override fun openSystemConfig(
        parent: Config?,
        fs: FS?
    ): FileBasedConfig = stripUrlSection(parent, delegate.openSystemConfig(parent, fs), fs)


    override fun openJGitConfig(
        parent: Config?,
        fs: FS?
    ): FileBasedConfig? = delegate.openJGitConfig(parent, fs)

    @Deprecated("Deprecated in Java")
    override fun getCurrentTime(): Long = delegate.getCurrentTime()

    @Deprecated("Deprecated in Java")
    override fun getTimezone(timestampMillis: Long): Int = delegate.getTimezone(timestampMillis)

}

private class UrlStrippingConfig(
    parent: Config?,
    file: File,
    fs: FS
) : FileBasedConfig(parent, file, fs) {
    override fun load() {
        super.load()

        getSubsections(ConfigConstants.CONFIG_KEY_URL)
            .forEach { unsetSection(ConfigConstants.CONFIG_KEY_URL, it) }
    }
}

private fun stripUrlSection(parent: Config?, original: FileBasedConfig, fs: FS?): FileBasedConfig {
    val file = original.file ?: return original // nothing readable to strip (eg. no home directory)
    return UrlStrippingConfig(parent, file, fs ?: FS.DETECTED)
}

// sets the system reader to one that bypasses url rewriting
fun jgitBypassUrlRewrite() {
    val current = SystemReader.getInstance()
    if (current is NoUrlRewriteSystemReader) return
    SystemReader.setInstance(NoUrlRewriteSystemReader(current))
}