package dev.jotalac.feature.git_sync.data

import dev.jotalac.feature.git_sync.domain.MailboxRepository
import dev.jotalac.feature.git_sync.domain.PushRejectedException
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/*
* What every platform's MailboxRepository has to do. A bare repository in a temp folder stands in for the mailbox on
* GitHub, folders for the copies of two devices - a local remote needs no credentials.
* */
abstract class MailboxRepositoryContractTest {

    protected abstract val mailbox: MailboxRepository

    /** An empty bare repository at [path] - the mailbox. */
    protected abstract fun createRemote(path: String)

    private val folder = TestFolder("mailbox-test")
    private val remote = folder.path("remote.git")

    @BeforeTest
    fun setUp() = createRemote(remote)

    @AfterTest
    fun cleanUp() = folder.delete()

    @Test
    fun `publishing after another device published is rejected until the copy is updated`() = runTest {
        val laptop = folder.path("laptop")
        val phone = folder.path("phone")
        mailbox.create(laptop, remote).getOrThrow()
        mailbox.create(phone, remote).getOrThrow()
        folder.write(laptop, "bundles/000001.bundle.enc", "from the laptop")
        mailbox.publish(laptop, "bundle 1", "token", "user").getOrThrow()

        // the phone has not seen the laptop's bundle - its push would overwrite it
        folder.write(phone, "bundles/000001.bundle.enc", "from the phone")
        val rejected = mailbox.publish(phone, "bundle 1", "token", "user")
        assertIs<PushRejectedException>(rejected.exceptionOrNull())

        // updating drops what the phone could not publish and takes over the laptop's bundle
        mailbox.update(phone, "token", "user").getOrThrow()
        assertEquals("from the laptop", folder.read(phone, "bundles/000001.bundle.enc"))
        folder.write(phone, "bundles/000002.bundle.enc", "from the phone")
        assertTrue(mailbox.publish(phone, "bundle 2", "token", "user").isSuccess)
    }

    @Test
    fun `updating the copy of an empty mailbox leaves it empty`() = runTest {
        val copy = folder.path("copy")
        mailbox.create(copy, remote).getOrThrow()

        mailbox.update(copy, "token", "user").getOrThrow()

        assertTrue(mailbox.isEmpty(copy).getOrThrow())
        assertFalse(SystemFileSystem.exists(Path(copy, "bundles")))
    }

    @Test
    fun `a copy is not empty once something was published`() = runTest {
        val copy = folder.path("copy")
        mailbox.create(copy, remote).getOrThrow()
        folder.write(copy, "gitwriter-encryption.json", "{}")

        mailbox.publish(copy, "encrypt notebook", "token", "user").getOrThrow()

        assertFalse(mailbox.isEmpty(copy).getOrThrow())
    }
}
