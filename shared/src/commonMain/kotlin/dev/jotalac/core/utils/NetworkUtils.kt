package dev.jotalac.core.utils

import dev.jotalac.feature.git_sync.domain.isSshRemote
import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.http.Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/** Whether the notebook's git remote can be reached right now. */
enum class RemoteStatus {
    /** the device has no network at all */
    OFFLINE,

    /** there is a network, but the remote's server did not answer (server down, captive portal...) */
    UNREACHABLE,
    REACHABLE,
}

/**
 * What the platform knows about the network: true / false when it can tell (the OS on Android and iOS),
 * null when it cannot (desktop) - then only a request to the remote tells. Every emission is also a moment
 * to check the remote again.
 */
fun interface NetworkMonitor {
    fun networkAvailability(): Flow<Boolean?>
}

fun interface RemoteReachability {
    suspend fun canReach(remoteUrl: String): Boolean
}

/** One request to the remote's server - any HTTP answer (even 401 or 404) means it can be reached. */
class HttpRemoteReachability(private val client: HttpClient) : RemoteReachability {

    override suspend fun canReach(remoteUrl: String): Boolean {
        if (isSshRemote(remoteUrl)) return true
        val url = suspendRunCatching { Url(remoteUrl) }.getOrNull() ?: return false
        return withTimeoutOrNull(5.seconds) {
            suspendRunCatching { client.head("${url.protocol.name}://${url.host}") }.isSuccess
        } ?: false
    }
}

/**
 * Keeps track of whether a remote can be reached. The remote is checked with one request when it changes,
 * when the network changes and when [checkNow] is called - there is no polling of its own, so on mobile
 * nothing runs while the network stays the same.
 */
class RemoteStatusMonitor(
    private val reachability: RemoteReachability,
    private val networkMonitor: NetworkMonitor,
) {
    private val checkRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Checks the remote again, e.g. when the app comes back to the foreground. */
    fun checkNow() {
        checkRequests.tryEmit(Unit)
    }

    /** The status of [remoteUrl] (the active notebook's), null while there is no remote to check. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun status(remoteUrl: Flow<String?>): Flow<RemoteStatus?> =
        combine(
            remoteUrl.distinctUntilChanged(),
            networkMonitor.networkAvailability(),
            checkRequests.onStart { emit(Unit) },
        ) { url, network, _ -> url to network }
            // a newer change cancels a check still waiting for its answer
            .mapLatest { (url, network) ->
                when {
                    url.isNullOrBlank() -> null
                    network == false -> RemoteStatus.OFFLINE
                    reachability.canReach(url) -> RemoteStatus.REACHABLE
                    else -> RemoteStatus.UNREACHABLE
                }
            }
            .distinctUntilChanged()
}
