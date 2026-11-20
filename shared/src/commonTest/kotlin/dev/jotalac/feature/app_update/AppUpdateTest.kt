package dev.jotalac.feature.app_update

import dev.jotalac.feature.app_update.data.AppUpdateRepository
import dev.jotalac.feature.app_update.data.parseLatestRelease
import dev.jotalac.feature.app_update.domain.LatestRelease
import dev.jotalac.feature.app_update.domain.isNewerVersion
import dev.jotalac.testing.fakes.InMemoryDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class AppUpdateTest {

    // --- versions

    @Test
    fun `versions are compared as numbers`() {
        assertTrue(isNewerVersion("1.2.10", "1.2.9"))
        assertTrue(isNewerVersion("1.3", "1.2.9"))
        assertTrue(isNewerVersion("2.0.0", "1.99.99"))
        assertFalse(isNewerVersion("1.2.9", "1.2.10"))
    }

    @Test
    fun `the same version or an older one is no update`() {
        assertFalse(isNewerVersion("1.2.2", "1.2.2"))
        assertFalse(isNewerVersion("1.2", "1.2.0"))
        // a build newer than the latest release
        assertFalse(isNewerVersion("1.2.2", "1.3.0"))
    }

    @Test
    fun `a leading v and a pre-release suffix do not matter`() {
        assertTrue(isNewerVersion("v1.2.3", "1.2.2"))
        assertFalse(isNewerVersion("1.2.2-beta", "1.2.2"))
    }

    // --- the github answer

    @Test
    fun `the latest release is read from the github answer`() {
        val body = """{"tag_name":"1.3.0","html_url":"https://github.com/jotalac/git-writer/releases/tag/1.3.0",
            |"name":"GitWriter 1.3.0","assets":[]}""".trimMargin()

        assertEquals(
            LatestRelease("1.3.0", "https://github.com/jotalac/git-writer/releases/tag/1.3.0"),
            parseLatestRelease(body)
        )
    }

    // --- checking

    private val release = LatestRelease("1.3.0", "https://example.com/1.3.0")
    private var time = 0L
    private var fetches = 0
    private var failing = false
    private val repository = AppUpdateRepository(
        releaseSource = {
            fetches++
            if (failing) error("offline")
            release
        },
        dataStore = InMemoryDataStore(),
        now = { time },
    )

    @Test
    fun `a check remembers the latest release`() = runTest {
        assertEquals(release, repository.checkNow().getOrThrow())

        assertEquals(release, repository.state.first().latestRelease)
    }

    @Test
    fun `the automatic check runs once a day`() = runTest {
        repository.checkIfDue(INSTALLED)
        time += 23.hours.inWholeMilliseconds
        repository.checkIfDue(INSTALLED)
        assertEquals(1, fetches)

        time += 2.hours.inWholeMilliseconds
        repository.checkIfDue(INSTALLED)
        assertEquals(2, fetches)
    }

    @Test
    fun `a failed check is tried again on the next look`() = runTest {
        failing = true
        repository.checkIfDue(INSTALLED)
        assertNull(repository.state.first().latestRelease)

        failing = false
        time += 1.hours.inWholeMilliseconds
        repository.checkIfDue(INSTALLED)
        assertEquals(2, fetches)
        assertEquals(release, repository.state.first().latestRelease)
    }

    @Test
    fun `a failed manual check keeps the release found before`() = runTest {
        repository.checkNow()
        failing = true

        assertTrue(repository.checkNow().isFailure)
        assertEquals(release, repository.state.first().latestRelease)
    }

    @Test
    fun `a newer release is reported by every daily check`() = runTest {
        assertEquals(release, repository.checkIfDue(INSTALLED))

        // not due again the same day
        time += 1.hours.inWholeMilliseconds
        assertNull(repository.checkIfDue(INSTALLED))

        // the next day the same release is reported again
        time += 24.hours.inWholeMilliseconds
        assertEquals(release, repository.checkIfDue(INSTALLED))
    }

    @Test
    fun `a release that is not newer is not announced`() = runTest {
        assertNull(repository.checkIfDue("1.3.0"))
        assertNull(repository.checkIfDue("2.0.0"))
    }

    private companion object {
        const val INSTALLED = "1.2.2"
    }
}
