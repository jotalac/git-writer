package dev.jotalac.feature.git_sync.domain

import dev.jotalac.core.utils.isDesktopPlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoteUrlsTest {

    private val ssh = "git@github.com:me/notes.git"
    private val https = "https://github.com/me/notes.git"

    @Test
    fun `ssh remotes in the short and the url form`() {
        assertTrue(isSshRemote(ssh))
        assertTrue(isSshRemote("  git@gitea.home.lan:me/notes.git "))
        assertTrue(isSshRemote("ssh://git@gitea.home.lan:2222/me/notes.git"))
        assertTrue(isSshRemote("git+ssh://git@host/me/notes.git"))
    }

    @Test
    fun `http remotes and local paths and other text are not ssh`() {
        assertFalse(isSshRemote(https))
        assertFalse(isSshRemote("https://user@github.com/me/notes.git"))
        assertFalse(isSshRemote("/Users/me/notes.git"))
        assertFalse(isSshRemote("C:\\notes\\remote.git"))
        assertFalse(isSshRemote(""))
        assertFalse(isSshRemote(null))
    }

    @Test
    fun `only the desktop app reaches ssh remotes`() {
        assertEquals(isDesktopPlatform, isSshSupported)
    }

    @Test
    fun `an http remote needs a username and a token - an ssh one neither`() {
        assertTrue(hasRemoteCredentials(https, "me", "token"))
        assertFalse(hasRemoteCredentials(https, "me", ""))
        assertFalse(hasRemoteCredentials(https, null, "token"))
        assertTrue(hasRemoteCredentials(ssh, null, null))
    }

    @Test
    fun `an http remote syncs with its token - an ssh one only where ssh is supported`() {
        assertTrue(canSync(https, "token"))
        assertFalse(canSync(https, ""))
        assertEquals(isSshSupported, canSync(ssh, null))
        assertFalse(canSync(null, "token"))
        assertFalse(canSync(" ", "token"))
    }
}
