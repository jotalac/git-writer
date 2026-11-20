package dev.jotalac.feature.app_update.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.jotalac.core.utils.suspendRunCatching
import dev.jotalac.feature.app_update.domain.AppUpdateState
import dev.jotalac.feature.app_update.domain.LatestRelease
import dev.jotalac.feature.app_update.domain.ReleaseSource
import dev.jotalac.feature.app_update.domain.isNewerVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Looks up the latest release and remembers it, so settings show it without asking again. Only checks - the update
 * itself is downloaded by the user.
 */
class AppUpdateRepository(
    private val releaseSource: ReleaseSource,
    private val dataStore: DataStore<Preferences>,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    val state: Flow<AppUpdateState> = dataStore.data.map { preferences ->
        val version = preferences[LATEST_VERSION]
        val url = preferences[LATEST_URL]
        AppUpdateState(
            latestRelease = if (version != null && url != null) LatestRelease(version, url) else null,
            lastCheckedAtMillis = preferences[LAST_CHECKED_AT],
        )
    }

    suspend fun checkNow(): Result<LatestRelease> = suspendRunCatching {
        val release = releaseSource.fetchLatestRelease()
        dataStore.edit { preferences ->
            preferences[LATEST_VERSION] = release.version
            preferences[LATEST_URL] = release.url
            preferences[LAST_CHECKED_AT] = now()
        }
        release
    }

    // everyday check for newer version
    suspend fun checkIfDue(installedVersion: String): LatestRelease? {
        val lastChecked = dataStore.data.first()[LAST_CHECKED_AT]
        // a clock set back also makes the check due
        val isDue = lastChecked == null || now() - lastChecked !in 0 until CHECK_INTERVAL_MILLIS
        if (!isDue) return null

        val release = checkNow().getOrNull() ?: return null
        return release.takeIf { isNewerVersion(it.version, installedVersion) }
    }

    private companion object {
        val LATEST_VERSION = stringPreferencesKey("update_latest_version")
        val LATEST_URL = stringPreferencesKey("update_latest_url")
        val LAST_CHECKED_AT = longPreferencesKey("update_last_checked_at")
        val CHECK_INTERVAL_MILLIS = 0.days.inWholeMilliseconds
    }
}
