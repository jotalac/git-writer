package dev.jotalac.feature.git_sync.data

import dev.jotalac.feature.git_sync.domain.GitSyncRepository
import dev.jotalac.feature.git_sync.domain.MirrorRepository
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/*
* What every platform's MirrorRepository has to do - JGit on desktop and Android, libgit2 on iOS. Bundles move the
* commits of an encrypted notebook, so a bundle made on one platform has to import on the other: both are also tested
* against bundles git itself made. Runs against real repositories in a temp folder.
* */
abstract class MirrorRepositoryContractTest {

    protected abstract val mirror: MirrorRepository
    protected abstract val sync: GitSyncRepository

    /** Commits everything in the working tree at [path]. */
    protected abstract fun commitAll(path: String)

    private val folder = TestFolder("mirror-test")

    @AfterTest
    fun cleanUp() = folder.delete()

    @Test
    fun `all commits of a notebook rebuild it in an empty mirror`() = runTest {
        val laptop = notebook("laptop")
        folder.write(laptop, "a.md", "first")
        commitAll(laptop)
        folder.write(laptop, "b.md", "second")
        commitAll(laptop)
        val mirrorPath = emptyMirror()

        val head = mirror.importBundle(mirrorPath, mirror.createBundle(laptop, since = null).getOrThrow()).getOrThrow()

        assertEquals(mirror.head(laptop).getOrThrow(), head)
        val copy = checkout(mirrorPath)
        assertEquals("first", folder.read(copy, "a.md"))
        assertEquals("second", folder.read(copy, "b.md"))
    }

    @Test
    fun `newer commits travel in a bundle of their own on top of the earlier ones`() = runTest {
        val laptop = notebook("laptop")
        folder.write(laptop, "a.md", "first")
        commitAll(laptop)
        val mirrorPath = emptyMirror()
        val firstHead = mirror.importBundle(mirrorPath, mirror.createBundle(laptop, since = null).getOrThrow()).getOrThrow()
        folder.write(laptop, "b.md", "second")
        commitAll(laptop)

        val next = mirror.createBundle(laptop, since = firstHead).getOrThrow()
        val head = mirror.importBundle(mirrorPath, next).getOrThrow()

        assertEquals(listOf(firstHead), parseBundleHeader(next).prerequisites)
        assertEquals(mirror.head(laptop).getOrThrow(), head)
        assertEquals("second", folder.read(checkout(mirrorPath), "b.md"))
    }

    @Test
    fun `a bundle is refused by a mirror without the commits it builds on`() = runTest {
        // importing them in the wrong order must not leave main pointing at a broken history
        val mirrorPath = emptyMirror()

        val result = mirror.importBundle(mirrorPath, GitBundleFixtures.NEXT)

        assertTrue(result.isFailure)
        assertNull(mirror.head(mirrorPath).getOrThrow())
    }

    @Test
    fun `bundles made by git itself import`() = runTest {
        val mirrorPath = emptyMirror()

        assertEquals(GitBundleFixtures.FIRST_COMMIT, mirror.importBundle(mirrorPath, GitBundleFixtures.FULL).getOrThrow())
        assertEquals(GitBundleFixtures.SECOND_COMMIT, mirror.importBundle(mirrorPath, GitBundleFixtures.NEXT).getOrThrow())

        val copy = checkout(mirrorPath)
        assertEquals("first note\n", folder.read(copy, "a.md"))
        assertEquals("second note\n", folder.read(copy, "b.md"))
    }

    private suspend fun notebook(name: String): String {
        val path = folder.path(name)
        SystemFileSystem.createDirectories(Path(path))
        sync.initRepository(path).getOrThrow()
        return path
    }

    private suspend fun emptyMirror(): String = folder.path("mirror.git").also { mirror.create(it).getOrThrow() }

    private suspend fun checkout(mirrorPath: String): String {
        val path = folder.path("checkout-${Random.nextInt(0, Int.MAX_VALUE)}")
        sync.cloneRepository(mirrorPath, "token", path, "user").getOrThrow()
        return path
    }
}
