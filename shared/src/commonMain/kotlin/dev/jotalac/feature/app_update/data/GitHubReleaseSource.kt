package dev.jotalac.feature.app_update.data

import dev.jotalac.feature.app_update.domain.LatestRelease
import dev.jotalac.feature.app_update.domain.ReleaseSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

private const val LATEST_RELEASE_URL = "https://api.github.com/repos/jotalac/git-writer/releases/latest"

/** The latest release on GitHub - drafts and pre-releases are never "latest". */
class GitHubReleaseSource(private val client: HttpClient) : ReleaseSource {

    override suspend fun fetchLatestRelease(): LatestRelease {
        val body = withTimeoutOrNull(15.seconds) {
            val response = client.get(LATEST_RELEASE_URL) {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                header(HttpHeaders.UserAgent, "GitWriter")
            }
            check(response.status.isSuccess()) { "GitHub answered ${response.status}" }
            response.bodyAsText()
        } ?: error("GitHub did not answer in time")

        return parseLatestRelease(body)
    }
}

@Serializable
private class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
)

private val ReleaseJson = Json { ignoreUnknownKeys = true }

internal fun parseLatestRelease(body: String): LatestRelease {
    val release = ReleaseJson.decodeFromString<GitHubRelease>(body)
    return LatestRelease(version = release.tagName.removePrefix("v"), url = release.htmlUrl)
}
